#!/bin/sh

python src/train.py --cuda=True --pretrained-model=bert-base-romanian-uncased-v1 --freeze-bert=False --lstm-dim=-1 \
--language=romanian --seed=1 --lr=5e-6 --epoch=10 --use-crf=False --augment-type=all  --augment-rate=0.15 \
--alpha-sub=0.4 --alpha-del=0.4 --data-path=data --save-path=out_ro_marcell10

