# -*- coding: utf-8 -*-
"""
Created on Fri Aug 19 08:39:20 2022

@author: LKJ
"""
import torch
import torch.nn as nn
import math
import torch.utils.model_zoo as model_zoo
from torch.nn import functional as F
from torchtext.vocab import vocab
from collections import Counter, OrderedDict
from torch.utils.data import Dataset, DataLoader
from torchtext.transforms import VocabTransform  # 注意：torchtext版本0.12+
import torch.optim as optim
import itertools
import random
import numpy as np
import warnings
import sys
warnings.filterwarnings("ignore")
 
############################第1部分，CBOW模型，加载sql语句得到对应的句向量###############################
def get_text():
    # sentence_list = [  # 假设这是全部的训练语料
    #     "nlp drives computer programs that translate text from one language to another",
    #     "nlp combines computational linguistics rule based modeling of human language with statistical",
    #     "nlp model respond to text or voice data and respond with text",
    # ]
    p_sample = []
    f = open("data/p_sample.txt",encoding='utf-8')
    while True:
        line = f.readline()
        if line:
            split = line.split('\t')
            if len(split)==2 :
                line = split[1]
                p_sample.append(line)
        else:
            break
    f.close()
    n_sample = []
    f = open("data/n_sample.txt",encoding='utf-8')
    while True:
        line = f.readline()
        if line:
            split = line.split('\t')
            if len(split)==2 :
                line = split[1]
                n_sample.append(line)
        else:
            break
    f.close()
    sentence_list = p_sample + n_sample
    return sentence_list


class CbowDataSet(Dataset):
    def __init__(self, text_list, side_window=3):
        """
        构造Word2vec的CBOW采样Dataset
        :param text_list: 语料
        :param side_window: 单侧正例（构造背景词）采样数，总正例是：2 * side_window
        """
        super(CbowDataSet, self).__init__()
        self.side_window = side_window
        text_vocab, vocab_transform = self.reform_vocab(text_list)
        self.text_list = text_list  # 原始文本
        self.text_vocab = text_vocab  # torchtext的vocab
        self.vocab_transform = vocab_transform  # torchtext的vocab_transform
        self.cbow_data = self.generate_cbow()

    def __len__(self):
        return len(self.cbow_data)

    def __getitem__(self, idx):
        data_row = self.cbow_data[idx]
        return data_row[0], data_row[1]

    def reform_vocab(self, text_list):
        """根据语料构造torchtext的vocab"""
        total_word_list = []
        for _ in text_list:  # 将嵌套的列表([[xx,xx],[xx,xx]...])拉平 ([xx,xx,xx...])
            total_word_list += _.split("#")
        counter = Counter(total_word_list)  # 统计计数
        sorted_by_freq_tuples = sorted(counter.items(), key=lambda x: x[1], reverse=True)  # 构造成可接受的格式：[(单词,num), ...]
        ordered_dict = OrderedDict(sorted_by_freq_tuples)
        # 开始构造 vocab
        special_token = ["<UNK>", "<SEP>"]  # 特殊字符
        text_vocab = vocab(ordered_dict, specials=special_token)  # 单词转token，specials里是特殊字符，可以为空
        text_vocab.set_default_index(0)
        vocab_transform = VocabTransform(text_vocab)
        return text_vocab, vocab_transform

    def generate_cbow(self):
        """生成CBOW的训练数据"""
        cbow_data = []
        for sentence in self.text_list:
            sentence_id_list = np.array(self.vocab_transform(sentence.split('#')))
            for center_index in range(
                    self.side_window, len(sentence_id_list) - self.side_window):  # 防止前面或后面取不到足够的值，这是取index的上下界
                pos_index = list(range(center_index - self.side_window, center_index + self.side_window + 1))
                del pos_index[self.side_window]
                cbow_data.append([sentence_id_list[center_index], sentence_id_list[pos_index]])
        return cbow_data

    def get_vocab_transform(self):
        return self.vocab_transform

    def get_vocab_size(self):
        return len(self.text_vocab)


class Word2VecModel(nn.Module):
    def __init__(self, vocab_size, batch_size, word_embedding_size=32, hidden=64):
        """
        Word2vec模型CBOW实现
        :param vocab_size: 单词个数
        :param word_embedding_size: 每个词的词向量维度
        :param hidden: 隐层维度
        """
        super(Word2VecModel, self).__init__()
        self.vocab_size = vocab_size
        self.word_embedding_size = word_embedding_size
        self.hidden = hidden
        self.batch_size = batch_size
        self.word_embedding = nn.Embedding(self.vocab_size, self.word_embedding_size)  # token对应的embedding
        # 建模
        self.linear_in = nn.Linear(self.word_embedding_size, self.hidden)
        self.linear_out = nn.Linear(self.hidden, self.vocab_size)

    def forward(self, input_labels):
        around_embedding = self.word_embedding(input_labels)
        avg_around_embedding = torch.mean(around_embedding, dim=1)  # 1. 输入的词向量对应位置求平均
        in_emb = F.relu(self.linear_in(avg_around_embedding))  # 2. 过第一个linear，使用relu激活函数
        out_emb = F.log_softmax(self.linear_out(in_emb))  # 3. 过第二个linear，得到维度是：[batch_size, 单词总数]
        return out_emb

    def get_embedding(self, token_list: list):
        return self.word_embedding(torch.Tensor(token_list).long())
    
    
#######################################第2部分，resnet模型，进行句向量的分类####################################
def conv3x3(in_planes, out_planes, stride=1):
    "3x3 convolution with padding"
    return nn.Conv2d(in_planes, out_planes, kernel_size=3, stride=stride,
                     padding=1, bias=False)
 
class ChannelAttention(nn.Module):
    def __init__(self, in_planes, ratio=16):
        super(ChannelAttention, self).__init__()
        self.avg_pool = nn.AdaptiveAvgPool2d(1)
        self.max_pool = nn.AdaptiveMaxPool2d(1)
 
        self.fc1   = nn.Conv2d(in_planes, in_planes // 16, 1, bias=False)
        self.relu1 = nn.ReLU()
        self.fc2   = nn.Conv2d(in_planes // 16, in_planes, 1, bias=False)
 
        self.sigmoid = nn.Sigmoid()
 
    def forward(self, x):
        avg_out = self.fc2(self.relu1(self.fc1(self.avg_pool(x))))
        max_out = self.fc2(self.relu1(self.fc1(self.max_pool(x))))
        out = avg_out + max_out
        return self.sigmoid(out)
 
class SpatialAttention(nn.Module):
    def __init__(self, kernel_size=7):
        super(SpatialAttention, self).__init__()
 
        assert kernel_size in (3, 7), 'kernel size must be 3 or 7'
        padding = 3 if kernel_size == 7 else 1
 
        self.conv1 = nn.Conv2d(2, 1, kernel_size, padding=padding, bias=False)
        self.sigmoid = nn.Sigmoid()
 
    def forward(self, x):
        avg_out = torch.mean(x, dim=1, keepdim=True)
        max_out, _ = torch.max(x, dim=1, keepdim=True)
        x = torch.cat([avg_out, max_out], dim=1)
        x = self.conv1(x)
        return self.sigmoid(x)
 
class BasicBlock(nn.Module):
    expansion = 1
 
    def __init__(self, inplanes, planes, stride=1, downsample=None):
        super(BasicBlock, self).__init__()
        self.conv1 = conv3x3(inplanes, planes, stride)
        self.bn1 = nn.BatchNorm2d(planes)
        self.relu = nn.ReLU(inplace=True)
        self.conv2 = conv3x3(planes, planes)
        self.bn2 = nn.BatchNorm2d(planes)
 
        self.ca = ChannelAttention(planes)
        self.sa = SpatialAttention()
 
        self.downsample = downsample
        self.stride = stride
 
    def forward(self, x):
        residual = x
 
        out = self.conv1(x)
        out = self.bn1(out)
        out = self.relu(out)
 
        out = self.conv2(out)
        out = self.bn2(out)
 
        out = self.ca(out) * out
        out = self.sa(out) * out
 
        if self.downsample is not None:
            residual = self.downsample(x)
 
        out += residual
        out = self.relu(out)
 
        return out
 
 
class Bottleneck(nn.Module):
    expansion = 4
 
    def __init__(self, inplanes, planes, stride=1, downsample=None):
        super(Bottleneck, self).__init__()
        self.conv1 = nn.Conv2d(inplanes, planes, kernel_size=1, bias=False)
        self.bn1 = nn.BatchNorm2d(planes)
        self.conv2 = nn.Conv2d(planes, planes, kernel_size=3, stride=stride,
                               padding=1, bias=False)
        self.bn2 = nn.BatchNorm2d(planes)
        self.conv3 = nn.Conv2d(planes, planes * 4, kernel_size=1, bias=False)
        self.bn3 = nn.BatchNorm2d(planes * 4)
        self.relu = nn.ReLU(inplace=True)
 
        self.ca = ChannelAttention(planes * 4)
        self.sa = SpatialAttention()
 
        self.downsample = downsample
        self.stride = stride
 
    def forward(self, x):
        residual = x
 
        out = self.conv1(x)
        out = self.bn1(out)
        out = self.relu(out)
 
        out = self.conv2(out)
        out = self.bn2(out)
        out = self.relu(out)
 
        out = self.conv3(out)
        out = self.bn3(out)
 
        out = self.ca(out) * out
        out = self.sa(out) * out
 
        if self.downsample is not None:
            residual = self.downsample(x)
 
        out += residual
        out = self.relu(out)
 
        return out
 
 
class ResNet(nn.Module):
 
    def __init__(self, block, layers, num_classes=2):
        self.inplanes = 32
        super(ResNet, self).__init__()
        self.conv1 = nn.Conv2d(1, 32, kernel_size=3, stride=1, padding=3,
                               bias=False)
        self.bn1 = nn.BatchNorm2d(64)
        self.relu = nn.ReLU(inplace=True)
        self.maxpool = nn.MaxPool2d(kernel_size=3, stride=2, padding=1)
        self.layer1 = self._make_layer(block, 16, layers[0])
        self.layer2 = self._make_layer(block, 16, layers[1], stride=2)
        self.layer3 = self._make_layer(block, 16, layers[2], stride=2)
        self.layer4 = self._make_layer(block, 16, layers[3], stride=2)
        self.avgpool = nn.AvgPool2d(7, stride=1)
        self.fc = nn.Linear(400 * block.expansion, num_classes)
 
        for m in self.modules():
            if isinstance(m, nn.Conv2d):
                n = m.kernel_size[0] * m.kernel_size[1] * m.out_channels
                m.weight.data.normal_(0, math.sqrt(2. / n))
            elif isinstance(m, nn.BatchNorm2d):
                m.weight.data.fill_(1)
                m.bias.data.zero_()
 
    def _make_layer(self, block, planes, blocks, stride=1):
        downsample = None
        if stride != 1 or self.inplanes != planes * block.expansion:
            downsample = nn.Sequential(
                nn.Conv2d(self.inplanes, planes * block.expansion,
                          kernel_size=1, stride=stride, bias=False),
                nn.BatchNorm2d(planes * block.expansion),
            )
 
        layers = []
        layers.append(block(self.inplanes, planes, stride, downsample))
        self.inplanes = planes * block.expansion
        for i in range(1, blocks):
            layers.append(block(self.inplanes, planes))
 
        return nn.Sequential(*layers)
 
    def forward(self, x):
        x = self.conv1(x)
        # x = self.bn1(x)
        x = self.relu(x)
        # x = self.maxpool(x)
 
        x = self.layer1(x)
        x = self.layer2(x)
        x = self.layer3(x)
        x = self.layer4(x)
 
        # x = self.avgpool(x)
        x = x.view(x.size(0), -1)
        x = self.fc(x)
 
        return x
 
 
def resnet18_cbam(pretrained=False, **kwargs):
    """Constructs a ResNet-18 model.
    Args:
        pretrained (bool): If True, returns a model pre-trained on ImageNet
    """
    model = ResNet(BasicBlock, [2, 2, 2, 2], **kwargs)

    return model    
    
########################################第三部分，执行分类#################################################### 
batch_size = 1
sentence_list = get_text()   
cbow_data_set = CbowDataSet(sentence_list)  # 构造 DataSet
word2vec_model = Word2VecModel(cbow_data_set.get_vocab_size(), batch_size)
word2vec_model.load_state_dict(torch.load('word2vec_model_final.pth'))   #加载预训练好的词向量映射模型

load_filename = sys.argv[1]
save_filename = sys.argv[2]



#读取样本
test_sample = []
# f = open("data\\test_sample.txt",encoding='utf-8') #加载正样本
f = open(load_filename,encoding='utf-8') #加载正样本
while True:
    line = f.readline()
    if line:
        # line = line.split('\t')[1]
        test_sample.append(line)
    else:
        break
f.close()   




# 定义是否使用GPU
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")    
mymodel = resnet18_cbam().to(device)

total = 0
correct = 0



#开始训练
for epoch in range(1):
    
    fc_loss_all = 0.0

    

    sum_loss = 0.0
    correct = 0.0
    total = 0.0
    acc = 0
   
    l=[]
    for i in range(len(test_sample)):
        sentence = test_sample[i].split('\t')[1]
        sentence = sentence.split('\n')[0]
        vocab_transform = cbow_data_set.get_vocab_transform()
        sentence_ids = vocab_transform(sentence.split('#'))
        sentence_embedding = word2vec_model.get_embedding(sentence_ids)
        
        w,h = sentence_embedding.shape
        if w < 32:
            pad = nn.ZeroPad2d(padding=(0, 0, 0, 32-w))
            sentence_embedding = pad(sentence_embedding)
            sentence_embedding = sentence_embedding.reshape(1,1,32,32)
        if w > 32:
            sentence_embedding = sentence_embedding.reshape(1,1,w,h)
            sentence_embedding = F.interpolate(sentence_embedding, size=[32, 32])    
            
        
        out = mymodel(sentence_embedding)
        _, predicted = torch.max(out.data, 1)
        out = F.softmax(out, dim=1) 
        p_value = out[0][predicted.detach().numpy()[0]].detach().numpy() 

        
        l.append(test_sample[i].split('\t')[0] + '\t' + str(p_value))
         
# f=open("data\\sqli_result.txt","w")  #将预测结果存放到txt文件中
f=open(save_filename,"w")  #将预测结果存放到txt文件中
 
for line in l:
    f.write(line+'\n')
f.close()

print('finish testing')


    
    
    
    # sentence = "se"
    # vocab_transform = cbow_data_set.get_vocab_transform()
    # sentence_ids = vocab_transform(sentence.split('#'))
    # sentence_embedding = word2vec_model.get_embedding(sentence_ids)
    
    # a = sentence_embedding
    # pad = nn.ZeroPad2d(padding=(0, 0, 0, 23))
    # a = pad(a)
    # a = a.reshape(1,1,32,32)
    # out = mymodel(a)
    
    # _, predicted = torch.max(out, 1)
    # labels = 0
    # total += 1
    # correct += (predicted.numpy() == labels).sum()
    # # a = torch.rand(1,1,64,32)
    # # a = F.interpolate(a, size=[32, 32])
    
    # print("准确率为：")
    # print(correct / total)