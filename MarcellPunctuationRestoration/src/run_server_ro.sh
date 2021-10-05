#!/bin/sh

source /data/vasile/PunctuationRestoration/venv_punctuation/bin/activate

python src/server.py --pretrained-model=bert-base-romanian-uncased-v1 --lstm-dim=-1 \
--use-crf=False \
--weight-path=out_ro_marcell10/weights.pt \
--sequence-length=256 \
--port 5105

