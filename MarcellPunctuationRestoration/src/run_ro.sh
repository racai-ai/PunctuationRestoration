#!/bin/sh

python src/run.py --pretrained-model=bert-base-romanian-uncased-v1 --lstm-dim=-1 \
--use-crf=False \
--data-path=data_test \
--save-path=out_test \
--weight-path=out_ro_marcell10/weights.pt \
--sequence-length=256

