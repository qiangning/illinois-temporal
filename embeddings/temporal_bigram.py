import os
import time
import math
import pickle as pkl
from utils import *

class temporal_bigram:
    def __init__(self):
        self.bigram_rel_counts = {}
        self.bigram_counts = {}
        self.vocab = set()
        self.relationSet = set(['before','after'])
        self.total_counts = 0
        self.total_bigrams = 0
    def save(self, path):
        pkl.dump(self, open(path, 'wb'))
    def addWord(self,word):
        if word not in self.vocab:
            self.vocab.add(word)
    def addRelation(self,rel):
        if rel not in self.relationSet:
            self.relationSet.add(rel)
    def addOneRelation(self,v1,v2,rel):
        self.addWord(v1)
        self.addWord(v2)
        self.addRelation(rel)
        if v1 not in self.bigram_rel_counts:
            self.bigram_rel_counts[v1] = {}
            self.bigram_counts[v1] = {}
        if v2 not in self.bigram_rel_counts[v1]:
            self.bigram_rel_counts[v1][v2] = {}
            self.bigram_counts[v1][v2] = 0
            self.total_bigrams += 1
        if rel not in self.bigram_rel_counts[v1][v2]:
            self.bigram_rel_counts[v1][v2][rel] = 0
        self.bigram_rel_counts[v1][v2][rel] += 1
        self.bigram_counts[v1][v2] += 1
        self.total_counts += 1
    def getBigramCounts(self,v1,v2):
        if v1 not in self.bigram_counts or v2 not in self.bigram_counts[v1]:
            return 0
        return self.bigram_counts[v1][v2]
    def getBigramRelCounts(self,v1,v2,rel):
        if v1 not in self.bigram_rel_counts or v2 not in self.bigram_rel_counts[v1] or rel not in self.bigram_rel_counts[v1][v2]:
            return 0
        return self.bigram_rel_counts[v1][v2][rel]
    def snapshot(self,pairs2monitor=None):
        print("-------------temporal_bigram: basic stats-------------",flush=True)
        print("Length of vocab: %d" % len(self.vocab),flush=True)
        print("Relation set: %s" % str(self.relationSet),flush=True)
        print("Total bigrams added: %d" % self.total_bigrams,flush=True)
        print("Total counts added: %d" % self.total_counts,flush=True)
        print("------------------------------------------------------",flush=True)
        if pairs2monitor is not None:
            print("\t%s\t%s\t%s\t%s" %('Pairs (PBefore)'.ljust(20),'TotalCnt'.ljust(10),'TBefore'.ljust(10),'TAfter'.ljust(10)),flush=True)
            for i, eventPair in enumerate(pairs2monitor):
                v1, v2 = eventPair[0], eventPair[1]
                if v1 not in self.vocab or                 v2 not in self.vocab or                 v1 not in self.bigram_rel_counts or                 v2 not in self.bigram_rel_counts[v1]:
                    continue
                print("\t%s\t%s\t%-10d\t%-5d (%-5.2f%%)\t\t%-5d (%-5.2f%%)" \
                      %(v1.ljust(10),v2.ljust(10),\
                        self.getBigramCounts(v1,v2),\
                        self.getBigramRelCounts(v1,v2,'before'),\
                        100.0*self.getBigramRelCounts(v1,v2,'before')/(self.getBigramCounts(v1,v2)+1e-6),\
                        self.getBigramRelCounts(v1,v2,'after'),\
                        100.0*self.getBigramRelCounts(v1,v2,'after')/(self.getBigramCounts(v1,v2)+1e-6)),\
                      flush=True)
                print("\t%s\t%s\t%-10d\t%-5d (%-5.2f%%)\t\t%-5d (%-5.2f%%)" \
                      %(v2.ljust(10),v1.ljust(10),\
                        self.getBigramCounts(v2,v1),\
                        self.getBigramRelCounts(v2,v1,'before'),\
                        100.0*self.getBigramRelCounts(v2,v1,'before')/(self.getBigramCounts(v2,v1)+1e-6),\
                        self.getBigramRelCounts(v2,v1,'after'),\
                        100.0*self.getBigramRelCounts(v2,v1,'after')/(self.getBigramCounts(v2,v1)+1e-6)),\
                      flush=True)
            for eventPair in pairs2monitor:
                v1, v2 = eventPair[0], eventPair[1]
                if v1 not in self.vocab:
                    print('%s not in vocab!' % v1,flush=True)
                if v2 not in self.vocab:
                    print('%s not in vocab!' % v2,flush=True)
                if v1 not in self.bigram_rel_counts or v2 not in self.bigram_rel_counts[v1]:
                    print('(%s,%s) not in bigrams' % (v1,v2),flush=True)
            print("------------------------------------------------------",flush=True)
