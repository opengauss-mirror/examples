# 基于ONNXRuntime推理部署指南

本示例以CBLUE数据集微调得到的模型为例，提供了文本分类任务的部署代码，自定义数据集可参考实现。
在推理部署前需将微调后的动态图模型转换导出为静态图，详细步骤见静态图模型导出。

以下是本部分主要代码结构及说明：

```text
├── infer_classification.py   # 模型推理的参数设置
├── predictor.py              # 模型推理的处理流程
└── README.md
```

## 环境安装

ONNX模型转换和推理部署依赖于Paddle2ONNX和ONNXRuntime。其中Paddle2ONNX支持将Paddle静态图模型转化为ONNX模型格式。

#### GPU端
请先确保机器已正确安装NVIDIA相关驱动和基础软件，确保CUDA >= 11.2，CuDNN >= 8.2，并使用以下命令安装所需依赖:
```
python -m pip install -r requirements_gpu.tx
```
\* 如需使用半精度（FP16）部署，请确保GPU设备的CUDA计算能力 (CUDA Compute Capability) 大于7.0。

#### CPU端
请使用如下命令安装所需依赖:
```
python -m pip install -r requirements_cpu.txt
```
## GPU部署推理样例

请使用如下命令进行GPU上的部署，可用`use_fp16`开启**半精度部署推理加速**，可用`device_id`**指定GPU卡号**。

- 文本分类任务

```
python infer_classification.py --device gpu --device_id 0 --dataset KUAKE-QIC --model_path_prefix ../../export/inference
```

可支持配置的参数：

* `model_path_prefix`：必须，待推理模型路径前缀。
* `model_name_or_path`：选择预训练模型；默认为"ernie-health-chinese"。
* `dataset`：CBLUE中的训练数据集。
   * `文本分类任务`：包括KUAKE-QIC, KUAKE-QQR, KUAKE-QTR, CHIP-CTC, CHIP-STS, CHIP-CDN-2C；默认为KUAKE-QIC。
* `max_seq_length`：模型使用的最大序列长度，最大不能超过512；`关系抽取任务`默认为300，其余默认为128。
* `use_fp16`：选择是否开启FP16进行加速，仅在`devive=gpu`时生效；默认关闭。
* `batch_size`：批处理大小，请结合显存情况进行调整，若出现显存不足，请适当调低这一参数；默认为200。
* `device`: 选用什么设备进行训练，可选cpu、gpu；默认为gpu。
* `device_id`: 选择GPU卡号；默认为0。
* `data_file`：本地待预测数据文件；默认为None。

#### 本地数据集加载
如需使用本地数据集，请指定本地待预测数据文件 `data_file`，每行一条样例，单文本输入每句一行，双文本输入以`\t`分隔符隔开。例如

**ctc-data.txt**
```
在过去的6个月曾服用偏头痛预防性药物或长期服用镇痛药物者，以及有酒精依赖或药物滥用习惯者；
患有严重的冠心病、脑卒中，以及传染性疾病、精神疾病者；
活动性乙肝（包括大三阳或小三阳）或血清学指标（HBsAg或/和HBeAg或/和HBcAb）阳性者，丙肝、肺结核、巨细胞病毒、严重真菌感染或HIV感染；
...
```

## CPU部署推理样例

请使用如下命令进行CPU上的部署，可用`num_threads`**调整预测线程数量**。

- 文本分类任务

```
python infer_classification.py --device cpu --dataset KUAKE-QIC --model_path_prefix ../../export/inference
```

可支持配置的参数：

* `model_path_prefix`：必须，待推理模型路径前缀。
* `model_name_or_path`：选择预训练模型；默认为"ernie-health-chinese"。
* `dataset`：CBLUE中的训练数据集。
   * `文本分类任务`：包括KUAKE-QIC, KUAKE-QQR, KUAKE-QTR, CHIP-CTC, CHIP-STS, CHIP-CDN-2C；默认为KUAKE-QIC。
* `max_seq_length`：模型使用的最大序列长度，最大不能超过512；`关系抽取任务`默认为300，其余默认为128。
* `batch_size`：批处理大小，请结合显存情况进行调整，若出现显存不足，请适当调低这一参数；默认为200。
* `device`: 选用什么设备进行训练，可选cpu、gpu；默认为gpu。
* `num_threads`：cpu线程数，在`device=gpu`时影响较小；默认为cpu的物理核心数量。
* `data_file`：本地待预测数据文件，格式见[GPU部署推理样例](#本地数据集加载)中的介绍；默认为None。

## 性能与精度测试

本节提供了在CBLUE数据集上预测的性能和精度数据，以供参考。
在CPU上测试，得到的数据如下。

| 数据集      | 最大文本长度 | 精度评估指标 | FP32 指标值 | FP32 latency(ms) |
| ----------  | ------------ | ------------ | ---------- | ---------------- |
| KUAKE-QIC   | 128          | Accuracy     | 0.8046     | 37.72            |
| KUAKE-QTR   | 64           | Accuracy     | 0.6886     | 18.40            |
| KUAKE-QQR   | 64           | Accuracy     | 0.7755     | 10.34            |
| CHIP-CTC    | 160          | Macro F1     | 0.8445     | 47.43            |
| CHIP-STS    | 96           | Macro F1     | 0.8892     | 27.67            |
| CHIP-CDN-2C | 256          | Micro F1     | 0.8921     | 26.86            |
| CMeEE       | 128          | Micro F1     | 0.6469     | 37.59            |
| CMeIE       | 300          | Micro F1     | 0.5902     | 213.04           |
