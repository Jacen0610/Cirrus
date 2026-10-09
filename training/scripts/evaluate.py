"""评估 checkpoint：精度、每类召回与混淆矩阵。

用法:
    python scripts/evaluate.py --checkpoint runs/exp001/best.pt              # 测试集
    python scripts/evaluate.py --checkpoint runs/exp001/best.pt --wild      # 自拍野外集
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path

import torch
from torch.utils.data import DataLoader

from common import (
    CloudDataset,
    build_eval_transform,
    load_checkpoint,
    read_split_csv,
    seed_everything,
)


def collect_wild_rows(wild_dir: Path, labels: list[str]) -> list[tuple[str, int]]:
    rows = []
    for entry in sorted(wild_dir.iterdir()):
        if entry.is_dir() and entry.name.lower() in labels:
            label_index = labels.index(entry.name.lower())
            rows.extend((str(p.relative_to(wild_dir)), label_index) for p in sorted(entry.iterdir()))
    if not rows:
        raise SystemExit(
            f"未在 {wild_dir} 找到有效类别目录（应为 {labels} 下的子目录）"
        )
    return rows


@torch.no_grad()
def run(model, loader, device, labels) -> dict:
    confusion = [[0] * len(labels) for _ in labels]
    top1 = top3 = count = 0
    for images, targets in loader:
        images, targets = images.to(device), targets.to(device)
        outputs = model(images)
        top_indices = outputs.topk(3, dim=1).indices
        top1 += (targets == top_indices[:, 0]).sum().item()
        top3 += (targets.unsqueeze(1) == top_indices).any(dim=1).sum().item()
        count += targets.size(0)
        for target, pred in zip(targets.tolist(), top_indices[:, 0].tolist()):
            confusion[target][pred] += 1

    print(f"top1 = {top1 / count:.4f}  top3 = {top3 / count:.4f}  (n={count})\n")
    axis_label = "真实\\预测"
    header = "".join(f"{name[:6]:>8}" for name in labels)
    print(f"{axis_label:>10}{header}")
    for name, row in zip(labels, confusion):
        print(f"{name[:8]:>10}" + "".join(f"{value:>8}" for value in row))

    per_class_recall = {
        name: (row[i] / sum(row) if sum(row) else 0.0)
        for i, (name, row) in enumerate(zip(labels, confusion))
    }
    print("\n每类召回:")
    for name, recall in per_class_recall.items():
        print(f"  {name:<16}{recall:.4f}")
    return {
        "n": count,
        "top1": top1 / count,
        "top3": top3 / count,
        "per_class_recall": per_class_recall,
        "confusion": confusion,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", type=Path, default=Path("runs/exp001/best.pt"))
    parser.add_argument("--split", choices=["val", "test"], default="test")
    parser.add_argument("--wild", action="store_true", help="评估 data/wild 自拍野外集")
    parser.add_argument("--data-dir", type=Path, default=Path("data"))
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--num-workers", type=int, default=8)
    args = parser.parse_args()

    seed_everything()
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model, meta = load_checkpoint(args.checkpoint)
    model.to(device)

    if args.wild:
        rows = collect_wild_rows(args.data_dir / "wild", meta["labels"])
        root = args.data_dir / "wild"
        tag = "wild"
    else:
        rows = read_split_csv(args.data_dir / "splits" / f"{args.split}.csv")
        root = args.data_dir / "raw"
        tag = args.split

    loader = DataLoader(
        CloudDataset(
            rows=rows,
            root=root,
            transform=build_eval_transform(meta["img_size"], meta["mean"], meta["std"]),
        ),
        batch_size=args.batch_size,
        num_workers=args.num_workers,
    )
    print(f"评估 {args.checkpoint} @ {tag}\n")
    results = run(model, loader, device, meta["labels"])

    out_path = args.checkpoint.parent / f"eval_{tag}.json"
    out_path.write_text(json.dumps(results, indent=2, ensure_ascii=False))
    print(f"\n结果已写入 {out_path}")


if __name__ == "__main__":
    main()
