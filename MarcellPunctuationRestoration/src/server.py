import os
import torch
from tqdm import tqdm
import numpy as np

import argparse
from dataset import Dataset
from model import DeepPunctuation, DeepPunctuationCRF
from config import *


parser = argparse.ArgumentParser(description='Punctuation restoration test')
parser.add_argument('--cuda', default=True, type=lambda x: (str(x).lower() == 'true'), help='use cuda if available')
parser.add_argument('--pretrained-model', type=str, help='pretrained language model')
parser.add_argument('--lstm-dim', default=-1, type=int,
                    help='hidden dimension in LSTM layer, if -1 is set equal to hidden dimension in language model')
parser.add_argument('--use-crf', default=False, type=lambda x: (str(x).lower() == 'true'),
                    help='whether to use CRF layer or not')
#parser.add_argument('--data-path', default='data/test', type=str, help='path to test datasets')
parser.add_argument('--weight-path',  type=str, help='model weight path')
parser.add_argument('--sequence-length', default=256, type=int,
                    help='sequence length to use when preparing dataset (default 256)')
parser.add_argument('--batch-size', default=8, type=int, help='batch size (default: 8)')
parser.add_argument('--save-path', default='out/', type=str, help='model and log save directory')
parser.add_argument('--port', default=5105, type=int, help='server port (default: 5105)')

args = parser.parse_args()


# tokenizer
tokenizer = MODELS[args.pretrained_model][1].from_pretrained(args.pretrained_model)
token_style = MODELS[args.pretrained_model][3]

# Data Loaders
data_loader_params = {
    'batch_size': args.batch_size,
    'shuffle': False,
    'num_workers': 0
}


# logs
model_save_path = args.weight_path
log_path = os.path.join(args.save_path, 'logs_test.txt')

# Model
device = torch.device('cuda' if (args.cuda and torch.cuda.is_available()) else 'cpu')
if args.use_crf:
    deep_punctuation = DeepPunctuationCRF(args.pretrained_model, freeze_bert=False, lstm_dim=args.lstm_dim)
else:
    deep_punctuation = DeepPunctuation(args.pretrained_model, freeze_bert=False, lstm_dim=args.lstm_dim)
deep_punctuation.to(device)


def predict(data_loader):
    """
    :return: string with predictions for each token
    """
    num_iteration = 0
    deep_punctuation.eval()
    result=""
    with torch.no_grad():
        for x, y, att, y_mask in data_loader:
            x, y, att, y_mask = x.to(device), y.to(device), att.to(device), y_mask.to(device)
            y_mask = y_mask.view(-1)
            if args.use_crf:
                y_predict = deep_punctuation(x, att, y)
                y_predict = y_predict.view(-1)
                y = y.view(-1)
            else:
                y_predict = deep_punctuation(x, att)
                y = y.view(-1)
                y_predict = y_predict.view(-1, y_predict.shape[2])
                y_predict = torch.argmax(y_predict, dim=1).view(-1)
            num_iteration += 1
            y_mask = y_mask.view(-1)

            for i in range(y.shape[0]):
                if y_mask[i] == 0:
                    # we can ignore this because we know there won't be any punctuation in this position
                    # since we created this position due to padding or sub-word tokenization
                    continue

                result+=punctuation_dict_rev[y_predict[i].item()]+" "

                #prd = y_predict[i]
                #print(prd)

    return result

deep_punctuation.load_state_dict(torch.load(model_save_path))

from flask import Flask,request,jsonify

app = Flask(__name__)

@app.route("/api/v1.0/punctuation", methods=["GET","POST"])
def punctuation():
    if request.method=="POST":
        textf=request.files['text']
        text=textf.read().decode('utf-8')
    else:
        text=request.args.get("text")
        text=text.encode("latin1").decode("utf8")

    #print(text)

    r=predict( 
        torch.utils.data.DataLoader(
            Dataset(text, tokenizer=tokenizer, sequence_len=args.sequence_length,
                            token_style=token_style, is_train=False, mode='text'), 
            **data_loader_params) 
    )
    return jsonify({'status':'OK','result':r})

app.run(threaded=False, debug=False, host="127.0.0.1", port=args.port)

