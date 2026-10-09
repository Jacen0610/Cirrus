"""微调 MobileNetV4 于 TJNU 云图数据集。

用法（在 training/ 目录下）:
    python scripts/train.py --output runs/exp001
"""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path

import timm
import torch
import torch.nn.functional as F
from torch.optim import AdamW
from torch.optim.lr_scheduler import CosineAnnealingLR
from torch.utils.data import DataLoader
from torch.amp import GradScaler, autocast

from common import (
    CloudDataset,
    build_eval_transform,
    build_train_transform,
    load_labels,
    read_split_csv,
    seed_everything,
)


def top_k_correct(outputs: torch.Tensor, targets: torch.Tensor, k: int) -> int:
    return (targets.unsqueeze(1) == outputs.topk(k, dim=1).indices).any(dim=1).sum().item()


@torch.no_grad()
def evaluate(model, loader, device) -> tuple[float, float, float]:
    model.eval()
    loss_sum, count, top1, top3 = 0.0, 0, 0, 0
    for images, targets in loader:
        images, targets = images.to(device), targets.to(device)
        outputs = model(images)
        loss_sum += F.cross_entropy(outputs, targets, reduction="sum").item()
        count += targets.size(0)
        top1 += top_k_correct(outputs, targets, 1)
        top3 += top_k_correct(outputs, targets, 3)
    return loss_sum / count, top1 / count, top3 / count


def train_one_epoch(model, loader, optimizer, scaler, device, epoch) -> float:
    model.train()
    loss_sum, count = 0.0, 0
    for step, (images, targets) in enumerate(loader):
        images, targets = images.to(device), targets.to(device)
        optimizer.zero_grad(set_to_none=True)
        with autocast(device_type=device.type):
            loss = F.cross_entropy(model(images), targets)
        scaler.scale(loss).backward()
        scaler.step(optimizer)
        scaler.update()
        loss_sum += loss.item() * targets.size(0)
        count += targets.size(0)
        if step % 50 == 0:
            print(f"  epoch {epoch} step {step}/{len(loader)} loss={loss.item():.4f}")
    return loss_sum / count


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", default="mobilenetv4_conv_small")
    parser.add_argument("--data-dir", type=Path, default=Path("data"))
    parser.add_argument("--output", type=Path, default=Path("runs/exp001"))
    parser.add_argument("--epochs", type=int, default=30)
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--lr", type=float, default=3e-4)
    parser.add_argument("--num-workers", type=int, default=8)
    args = parser.parse_args()

    seed_everything()
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"device = {device}")

    labels = load_labels(args.data_dir / "splits")
    raw_dir = args.data_dir / "raw"
    model = timm.create_model(args.model, pretrained=True, num_classes=len(labels))
    data_config = timm.data.resolve_model_data_config(model)
    mean, std = data_config["mean"], data_config["std"]
    img_size = data_config.get("input_size", (3, 256, 256))[-1]
    print(f"model={args.model} img_size={img_size} mean={mean} std={std}")

    datasets = {
        split: CloudDataset(
            rows=read_split_csv(args.data_dir / "splits" / f"{split}.csv"),
            root=raw_dir,
            transform=(
                build_train_transform(img_size, mean, std)
                if split == "train"
                else build_eval_transform(img_size, mean, std)
            ),
        )
        for split in ("train", "val", "test")
    }
    loaders = {
        split: DataLoader(
            dataset,
            batch_size=args.batch_size,
            shuffle=(split == "train"),
            num_workers=args.num_workers,
            pin_memory=(device.type == "cuda"),
            persistent_workers=(args.num_workers > 0),
        )
        for split, dataset in datasets.items()
    }

    optimizer = AdamW(model.parameters(), lr=args.lr, weight_decay=0.01)
    scheduler = CosineAnnealingLR(optimizer, T_max=args.epochs)
    scaler = GradScaler(device.type)

    args.output.mkdir(parents=True, exist_ok=True)
    meta = {
        "model_name": args.model,
        "labels": labels,
        "img_size": img_size,
        "mean": list(mean),
        "std": list(std),
    }

    metrics_path = args.output / "metrics.csv"
    if not metrics_path.exists():
        metrics_path.write_text("epoch,train_loss,train_acc,val_loss,val_acc,val_top3\n")

    best_val_acc = -1.0
    for epoch in range(1, args.epochs + 1):
        print(f"== epoch {epoch}/{args.epochs} lr={scheduler.get_last_lr()[0]:.2e}")
        model.to(device)
        train_loss = train_one_epoch(model, loaders["train"], optimizer, scaler, device, epoch)

        # 训练集上的准确率（同一增强，衡量收敛趋势足够）
        train_eval = evaluate(model, loaders["train"], device)
        val_loss, val_acc, val_top3 = evaluate(model, loaders["val"], device)
        print(
            f"  train_loss={train_loss:.4f} "
            f"val_loss={val_loss:.4f} val_acc={val_acc:.4f} val_top3={val_top3:.4f}"
        )
        with metrics_path.open("a", newline="") as f:
            csv.writer(f).writerow(
                [epoch, f"{train_loss:.4f}", f"{train_eval[1]:.4f}",
                 f"{val_loss:.4f}", f"{val_acc:.4f}", f"{val_top3:.4f}"]
            )

        if val_acc > best_val_acc:
            best_val_acc = val_acc
            torch.save({"state_dict": model.state_dict(), "meta": meta}, args.output / "best.pt")
            print(f"  保存最优模型 (val_acc={val_acc:.4f})")
        torch.save({"state_dict": model.state_dict(), "meta": meta}, args.output / "last.pt")
        scheduler.step()

    print(f"训练完成，best val_acc={best_val_acc:.4f}，产物在 {args.output}")
    (args.output / "meta.json").write_text(json.dumps(meta, indent=2))


if __name__ == "__main__":
    main()
