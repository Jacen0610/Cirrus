"""整理 TJNU GRSCD 数据集：扫描 data/raw/<云属>/，生成 70/15/15 分层划分。

前置条件：从 Zenodo 下载并解压数据集，把七类图片目录放入 training/data/raw/，
目录名使用云属英文小写（altocumulus/cirrus/cirrostratus/cumulonimbus/cumulus/stratus/stratocumulus）。
"""

from __future__ import annotations

import argparse
import csv
import json
import random
from collections import Counter
from pathlib import Path

from common import EXPECTED_LABELS, IMAGE_EXTENSIONS, SEED

TRAIN_RATIO = 0.70
VAL_RATIO = 0.15


def collect_images(raw_dir: Path) -> dict[str, list[Path]]:
    by_class: dict[str, list[Path]] = {}
    for entry in sorted(raw_dir.iterdir()):
        if not entry.is_dir():
            continue
        name = entry.name.strip().lower()
        if name not in EXPECTED_LABELS:
            print(f"[警告] 忽略未知类别目录: {entry.name}（应为 {EXPECTED_LABELS} 之一）")
            continue
        images = sorted(
            p for p in entry.iterdir()
            if p.is_file() and p.suffix.lower() in IMAGE_EXTENSIONS
        )
        by_class[name] = images
    return by_class


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--raw-dir", type=Path, default=Path("data/raw"))
    parser.add_argument("--out-dir", type=Path, default=Path("data/splits"))
    args = parser.parse_args()

    by_class = collect_images(args.raw_dir)
    missing = [label for label in EXPECTED_LABELS if not by_class.get(label)]
    if missing:
        raise SystemExit(f"[错误] 缺少类别目录: {missing}\n请检查 data/raw 下的布局。")

    total = sum(len(v) for v in by_class.values())
    print(f"共 {total} 张图片，各类数量: { {k: len(v) for k, v in by_class.items()} }")

    rng = random.Random(SEED)
    splits: dict[str, list[tuple[str, int]]] = {"train": [], "val": [], "test": []}
    for label_index, label in enumerate(EXPECTED_LABELS):
        images = list(by_class[label])
        rng.shuffle(images)
        n_train = int(len(images) * TRAIN_RATIO)
        n_val = int(len(images) * VAL_RATIO)
        for i, image in enumerate(images):
            split = "train" if i < n_train else ("val" if i < n_train + n_val else "test")
            splits[split].append((str(image.relative_to(args.raw_dir)), label_index))

    args.out_dir.mkdir(parents=True, exist_ok=True)
    for split, rows in splits.items():
        rows.sort(key=lambda row: row[0])
        with (args.out_dir / f"{split}.csv").open("w", newline="", encoding="utf-8") as f:
            writer = csv.writer(f)
            writer.writerow(["path", "label"])
            writer.writerows(rows)
        counter = Counter(label for _, label in rows)
        print(f"{split}: {len(rows)} 张，各类 {dict(sorted(counter.items()))}")

    (args.out_dir / "labels.json").write_text(json.dumps(EXPECTED_LABELS, indent=2))
    print(f"划分与 labels.json 已写入 {args.out_dir}")


if __name__ == "__main__":
    main()
