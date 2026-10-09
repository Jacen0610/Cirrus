"""导出 tflite：float32 基线 + int8 全整数量化（PT2E），并部署到安卓 assets。

用法:
    python scripts/export.py --checkpoint runs/exp003/best.pt

产物:
    runs/<exp>/cloud_classifier_float.tflite
    runs/<exp>/cloud_classifier_int8.tflite   （量化成功时）
    runs/<exp>/export_report.json             （精度/体积对比，README 素材）
    ../app/src/main/assets/ 下:
        cloud_classifier.tflite      ← 生产用（当前固定 float32，App 预处理为浮点）
        cloud_classifier_float.tflite / cloud_classifier_int8.tflite ← Benchmark 页用
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


def make_eval_loader(meta, data_dir, split="val", batch_size=32):
    rows = read_split_csv(data_dir / "splits" / f"{split}.csv")
    return DataLoader(
        CloudDataset(
            rows=rows,
            root=data_dir / "raw",
            transform=build_eval_transform(meta["img_size"], meta["mean"], meta["std"]),
        ),
        batch_size=batch_size,
        num_workers=4,
    )


@torch.no_grad()
def quick_accuracy(model, loader) -> float:
    model.eval()
    correct = total = 0
    for images, targets in loader:
        outputs = model(images)
        correct += (outputs.argmax(dim=1) == targets).sum().item()
        total += targets.size(0)
    return correct / total


def export_float(model, meta, out_path: Path):
    converter = load_converter()

    sample = torch.zeros(1, 3, meta["img_size"], meta["img_size"])
    edge_model = converter.convert(model.eval(), (sample,))
    edge_model.export(str(out_path))
    print(f"float32 已导出: {out_path} ({out_path.stat().st_size / 1e6:.2f} MB)")


def export_int8(model, meta, out_path: Path, data_dir, calib_samples):
    """int8 静态量化（PT2E）：annotate → prepare → 校准 → convert → LiteRT 导出。

    依赖当前 litert-torch 的 quantize API（PT2EQuantizer + torchao spec），
    版本再变时按报错提示调整本函数。
    """
    converter = load_converter()
    from litert_torch.quantize import PT2EQuantizer
    from litert_torch.quantize.pt2e_quantizer_utils import QuantizationConfig
    from torch.ao.quantization import (
        MinMaxObserver,
        PerChannelMinMaxObserver,
        PlaceholderObserver,
    )
    from torchao.quantization.pt2e.quantizer import QuantizationSpec

    # torch 2.13 起 PT2E 从 torch.ao 迁到了 torchao，做双版本兼容
    try:
        from torch.ao.quantization.quantize_pt2e import convert_pt2e, prepare_pt2e
    except ImportError:
        from torchao.quantization.pt2e.quantize_pt2e import convert_pt2e, prepare_pt2e

    act_spec = QuantizationSpec(
        dtype=torch.int8,
        qscheme=torch.per_tensor_symmetric,
        quant_min=-128,
        quant_max=127,
        observer_or_fake_quant_ctr=MinMaxObserver,
    )
    weight_spec = QuantizationSpec(
        dtype=torch.int8,
        qscheme=torch.per_channel_symmetric,
        quant_min=-128,
        quant_max=127,
        observer_or_fake_quant_ctr=PerChannelMinMaxObserver.with_args(ch_axis=0),
    )

    quantizer = PT2EQuantizer().set_global(
        QuantizationConfig(
            input_activation=act_spec,
            output_activation=act_spec,
            weight=weight_spec,
            bias=QuantizationSpec(
                dtype=torch.float,
                observer_or_fake_quant_ctr=PlaceholderObserver.with_args(dtype=torch.float),
            ),
            is_qat=False,
            is_dynamic=False,
        )
    )

    prepared = quantize_pt2e.prepare_pt2e(model.eval(), quantizer)
    calibrator_rows = read_split_csv(data_dir / "splits" / "train.csv")[:calib_samples]
    calib_loader = DataLoader(
        CloudDataset(
            rows=calibrator_rows,
            root=data_dir / "raw",
            transform=build_eval_transform(meta["img_size"], meta["mean"], meta["std"]),
        ),
        batch_size=16,
        num_workers=4,
    )
    with torch.no_grad():
        for images, _ in calib_loader:
            prepared(images)

    quantized = quantize_pt2e.convert_pt2e(prepared)

    sample = torch.zeros(1, 3, meta["img_size"], meta["img_size"])
    edge_model = converter.convert(quantized, (sample,))
    edge_model.export(str(out_path))
    print(f"int8 已导出: {out_path} ({out_path.stat().st_size / 1e6:.2f} MB)")
    return quantized


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", type=Path, default=Path("runs/exp003/best.pt"))
    parser.add_argument("--data-dir", type=Path, default=Path("data"))
    parser.add_argument("--calib-samples", type=int, default=200)
    args = parser.parse_args()

    model, meta = load_checkpoint(args.checkpoint)
    if sorted(meta["labels"]) != sorted(EXPECTED_LABELS):
        raise SystemExit("checkpoint 类别与安卓端词表不一致，中止导出")

    out_dir = args.checkpoint.parent
    float_path = out_dir / "cloud_classifier_float.tflite"
    int8_path = out_dir / "cloud_classifier_int8.tflite"

    report = {"float_accuracy_val": quick_accuracy(model, make_eval_loader(meta, args.data_dir))}

    export_float(model, meta, float_path)

    quantized_model = None
    try:
        quantized_model = export_int8(model, meta, int8_path, args.data_dir, args.calib_samples)
        report["int8_accuracy_val"] = quick_accuracy(
            quantized_model, make_eval_loader(meta, args.data_dir)
        )
    except Exception as exc:  # noqa: BLE001
        import traceback

        log_path = out_dir / "export_int8_error.log"
        log_path.write_text(traceback.format_exc())
        print(f"[警告] int8 导出失败，仅部署 float32\n{exc}\n")
        print(f"完整 traceback 已写入 {log_path}")

    # 部署：生产模型固定 float32（App 端预处理目前为浮点输入）；
    # float/int8 双文件供 Benchmark 页对比
    APP_ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    deployed = APP_ASSETS_DIR / "cloud_classifier.tflite"
    shutil.copy2(float_path, deployed)
    shutil.copy2(float_path, APP_ASSETS_DIR / "cloud_classifier_float.tflite")
    if int8_path.exists():
        shutil.copy2(int8_path, APP_ASSETS_DIR / "cloud_classifier_int8.tflite")

    report["float_size_mb"] = round(float_path.stat().st_size / 1e6, 2)
    if int8_path.exists():
        report["int8_size_mb"] = round(int8_path.stat().st_size / 1e6, 2)
    (out_dir / "export_report.json").write_text(json.dumps(report, indent=2))

    print("导出报告:")
    print(json.dumps(report, indent=2))
    print(f"已部署 -> {APP_ASSETS_DIR}")


if __name__ == "__main__":
    main()
