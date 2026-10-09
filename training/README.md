# Cirrus 模型训练管线

微调 MobileNetV4 于 TJNU 地基云图数据集（GRSCD，12k 张 / 7 云属），导出量化 tflite 部署到安卓端。

## 环境

```bash
conda create -n cirrus python=3.11 -y
conda activate cirrus
# 先按 https://pytorch.org 安装与本机 CUDA 匹配的 torch/torchvision，然后:
pip install -r requirements.txt
```

验证 GPU 可用: `python -c "import torch; print(torch.cuda.is_available())"`

## 数据集

1. 从 [Zenodo: TJNU-Ground-based-Remote-Sensing-Cloud-Database](https://zenodo.org/records/3635497) 下载并解压；
2. 整理为如下布局（目录名 = 云属英文小写）:

```
training/data/raw/
├── altocumulus/
├── cirrus/
├── cirrostratus/
├── cumulonimbus/
├── cumulus/
├── stratus/
└── stratocumulus/
```

## 流程

```bash
cd training

# 1. 生成分层划分（70/15/15，固定种子）
python scripts/prepare_data.py

# 2. 微调（4060 Ti 约 30–60 分钟 / 30 epoch）
python scripts/train.py --output runs/exp001

# 3. 评估测试集
python scripts/evaluate.py --checkpoint runs/exp001/best.pt

# 4. （推荐）自拍野外测试集评估域间掉点
#    把手机拍的照片按类别放入 training/data/wild/<云属>/
python scripts/evaluate.py --checkpoint runs/exp001/best.pt --wild

# 5. 导出并部署到 App（自动写入 ../app/src/main/assets/）
python scripts/export.py --checkpoint runs/exp001/best.pt
```

## 产物说明

- `runs/<exp>/metrics.csv` — 每 epoch 的损失与精度曲线
- `runs/<exp>/best.pt` / `last.pt` — checkpoint（含模型名、类别表、输入尺寸、归一化参数）
- `runs/<exp>/eval_*.json` — 评估结果（top1/top3、每类召回、混淆矩阵）
- `runs/<exp>/cloud_classifier_{float,int8}.tflite` — 导出模型
- `../app/src/main/assets/cloud_classifier.tflite` + `labels.txt` — 部署产物

## 已知注意点

- **量化 API**: ai-edge-torch 的量化接口迭代较快，`export.py` 的 int8 路径在版本不符时会降级为 float32 并给出提示；如遇此情况，参照 [ai-edge-torch 文档](https://github.com/google-ai-edge/ai-edge-torch) 微调 `_make_calibrator` 即可。
- **类别词表**: 七个云属名与安卓端 `CloudGenus.wireName` 一一对应，`prepare_data.py` 与 `export.py` 都会做一致性校验，防止训练/部署两端词表漂移。
- 量化后建议重跑一次评估对比掉点（把 float/int8 模型分别评估，写进 README 的对比表）。
