"""Cirrus 训练管线共享工具：随机种子、数据集、模型构建与变换。"""

from __future__ import annotations

import json
import random
from pathlib import Path

import numpy as np
import timm
import torch
from PIL import Image
from torch.utils.data import Dataset

SEED = 42

# 与安卓端 CloudGenus.wireName 对齐的七个云属（有序即索引约定）
EXPECTED_LABELS = [
    "altocumulus",
    "cirrus",
    "cirrostratus",
    "cumulonimbus",
    "cumulus",
    "stratus",
    "stratocumulus",
]

IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp"}


def seed_everything(seed: int = SEED) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)


def load_labels(splits_dir: Path) -> list[str]:
    labels = json.loads((splits_dir / "labels.json").read_text())
    if sorted(labels) != EXPECTED_LABELS:
        raise RuntimeError(
            f"类别与安卓端词表不一致:\n  训练集: {sorted(labels)}\n  期望值: {EXPECTED_LABELS}"
        )
    return labels


def read_split_csv(path: Path) -> list[tuple[str, int]]:
    import csv

    with path.open(newline="", encoding="utf-8") as f:
        return [(row["path"], int(row["label"])) for row in csv.DictReader(f)]


class CloudDataset(Dataset):
    """按 (path, label) 行列表读取图片的数据集。"""

    def __init__(self, rows: list[tuple[str, int]], root: Path, transform):
        self.rows = rows
        self.root = root
        self.transform = transform

    def __len__(self) -> int:
        return len(self.rows)

    def __getitem__(self, index: int):
        rel_path, label = self.rows[index]
        image = Image.open(self.root / rel_path).convert("RGB")
        return self.transform(image), label


def load_checkpoint(checkpoint_path: Path) -> tuple[torch.nn.Module, dict]:
    """按 checkpoint 内的元数据重建模型并载入权重。"""
    payload = torch.load(checkpoint_path, map_location="cpu", weights_only=False)
    meta = payload["meta"]
    model = timm.create_model(
        meta["model_name"],
        pretrained=False,
        num_classes=len(meta["labels"]),
    )
    model.load_state_dict(payload["state_dict"])
    model.eval()
    return model, meta


def build_train_transform(img_size: int, mean: tuple, std: tuple):
    from torchvision import transforms as T

    return T.Compose(
        [
            T.RandomResizedCrop(img_size, scale=(0.7, 1.0), interpolation=T.InterpolationMode.BICUBIC),
            T.RandomHorizontalFlip(),
            T.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2),
            T.ToTensor(),
            T.Normalize(mean, std),
        ]
    )


def build_eval_transform(img_size: int, mean: tuple, std: tuple):
    from torchvision import transforms as T

    return T.Compose(
        [
            T.Resize((img_size, img_size), interpolation=T.InterpolationMode.BICUBIC),
            T.ToTensor(),
            T.Normalize(mean, std),
        ]
    )
