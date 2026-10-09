"""导出 tflite：float32 基线 + int8 全整数量化（尽力而为），并部署到安卓 assets。

用法:
    python scripts/export.py --checkpoint runs/exp001/best.pt

产物:
    runs/<exp>/cloud_classifier_float.tflite
    runs/<exp>/cloud_classifier_int8.tflite   （量化 API 可用时）
    ../app/src/main/assets/cloud_classifier.tflite  + labels.txt（自动拷贝）
"""

from __future__ import annotations

import argparse
import importlib
import json
import shutil
from pathlib import Path

import torch
from torch.utils.data import DataLoader

from common import (
    EXPECTED_LABELS,
    CloudDataset,
    build_eval_transform,
    load_checkpoint,
    read_split_csv,
)

APP_ASSETS_DIR = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "assets"


def load_converter():
    """转换器曾用名 ai_edge_torch，现名 litert_torch——按可用性加载。"""
    for module_name in ("ai_edge_torch", "litert_torch"):
        try:
            return importlib.import_module(module_name)
        except ImportError:
            continue
    raise RuntimeError(
        "未找到 PyTorch→LiteRT 转换包，请先: pip install litert-torch"
    )


def calibration_inputs(meta, data_dir, num_samples, batch_size):
    """从训练集取样本用于量化校准。"""
    rows = read_split_csv(data_dir / "splits" / "train.csv")[:num_samples]
    loader = DataLoader(
        CloudDataset(
            rows=rows,
            root=data_dir / "raw",
            transform=build_eval_transform(meta["img_size"], meta["mean"], meta["std"]),
        ),
        batch_size=batch_size,
        num_workers=4,
    )
    for images, _ in loader:
        yield (images,)


def export_float(model, meta, out_path: Path):
    ai_edge_torch = load_converter()

    sample = torch.zeros(1, 3, meta["img_size"], meta["img_size"])
    edge_model = ai_edge_torch.convert(model.eval(), (sample,))
    edge_model.export(str(out_path))
    print(f"float32 已导出: {out_path} ({out_path.stat().st_size / 1e6:.2f} MB)")


def export_int8(model, meta, out_path: Path, data_dir, num_samples):
    """int8 静态量化。转换器量化 API 迭代较快，
    这里按当前文档的写法实现；版本不符时会给出明确指引。"""
    ai_edge_torch = load_converter()

    sample = torch.zeros(1, 3, meta["img_size"], meta["img_size"])
    edge_model = ai_edge_torch.convert(model.eval(), (sample,))
    calibrator = _make_calibrator(edge_model, meta, data_dir, num_samples)
    quantized = calibrator.calibrate_and_convert()
    quantized.export(str(out_path))
    print(f"int8 已导出: {out_path} ({out_path.stat().st_size / 1e6:.2f} MB)")


def _make_calibrator(edge_model, meta, data_dir, num_samples):
    try:
        from ai_edge_torch.quantize.ptq import StaticRangeQuantizer
    except ImportError:
        from litert_torch.quantize.ptq import StaticRangeQuantizer

    try:
        calibrator = StaticRangeQuantizer(edge_model)
        samples = list(calibration_inputs(meta, data_dir, num_samples, batch_size=16))
        calibrator.prepare_samples(samples) if hasattr(calibrator, "prepare_samples") else None
        return calibrator
    except Exception as exc:  # noqa: BLE001
        raise RuntimeError(
            "ai-edge-torch 量化 API 与脚本预期不符。\n"
            "请查看 https://github.com/google-ai-edge/ai-edge-torch 当前文档，\n"
            "调整 export.py 中 _make_calibrator 的实现；"
            f"或先使用 float32 模型。原始错误: {exc}"
        ) from exc


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", type=Path, default=Path("runs/exp001/best.pt"))
    parser.add_argument("--data-dir", type=Path, default=Path("data"))
    parser.add_argument("--calib-samples", type=int, default=200)
    args = parser.parse_args()

    model, meta = load_checkpoint(args.checkpoint)
    if sorted(meta["labels"]) != sorted(EXPECTED_LABELS):
        raise SystemExit("checkpoint 类别与安卓端词表不一致，中止导出")

    out_dir = args.checkpoint.parent
    float_path = out_dir / "cloud_classifier_float.tflite"
    int8_path = out_dir / "cloud_classifier_int8.tflite"

    export_float(model, meta, float_path)

    deployed = float_path
    try:
        export_int8(model, meta, int8_path, args.data_dir, args.calib_samples)
        deployed = int8_path
    except Exception as exc:  # noqa: BLE001
        print(f"[警告] int8 导出失败，将部署 float32 版本\n{exc}\n")

    APP_ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    target = APP_ASSETS_DIR / "cloud_classifier.tflite"
    shutil.copy2(deployed, target)
    (APP_ASSETS_DIR / "labels.txt").write_text("\n".join(meta["labels"]) + "\n")
    print(f"已部署 {deployed.name} -> {target}")
    print("labels.txt（顺序即输出索引）:")
    print(json.dumps(meta["labels"], indent=2))


if __name__ == "__main__":
    main()
