import os
import sys
from tqdm import tqdm

INPUT="marcell_punctuation_restoration"
OUTPUT="marcell_corpus"

print("Processing folder [%s] "%(INPUT))


files=[]
for entry in os.scandir(INPUT):
    if not entry.path.endswith(".conllup"): continue
    files.append(entry.path)

train_start=0
train_end=int(len(files)*70/100)

dev_start=train_end
dev_end=train_end+int(len(files)*15/100)

test_start=dev_end
test_end=len(files)

def processFile(fpath, fout):
    with open(fpath, mode="r", encoding="utf-8") as fp:
        for line in fp:
            line=line.strip()
            if len(line)==0 or line.startswith("#"): continue
            data=line.split("\t")
            word=data[1].lower()
            type=data[-1]
            
            if type=="PeriodAfter=yes": type="PERIOD"
            elif type=="CommaAfter=yes": type="COMMA"
            else: type="O"
            
            fout.write("%s\t%s\n"%(word,type))

fname="%s_train.data"%(OUTPUT)
print("Creating [%s] with [%d] files"%(fname,train_end-train_start))
fout=open(fname,mode="w", encoding="utf-8")
for i in tqdm(range(train_start,train_end)):
    processFile(files[i],fout)
fout.close()

fname="%s_dev.data"%(OUTPUT)
print("Creating [%s] with [%d] files"%(fname,dev_end-dev_start))
fout=open(fname,mode="w", encoding="utf-8")
for i in tqdm(range(dev_start,dev_end)):
    processFile(files[i],fout)
fout.close()

fname="%s_test.data"%(OUTPUT)
print("Creating [%s] with [%d] files"%(fname,test_end-test_start))
fout=open(fname,mode="w", encoding="utf-8")
for i in tqdm(range(test_start,test_end)):
    processFile(files[i],fout)
fout.close()
