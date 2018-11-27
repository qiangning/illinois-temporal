import os
from gensim.models import Word2Vec
from nltk.stem.wordnet import WordNetLemmatizer

docs = []
INPUT_DIRECTORY = "/scratch/sanjay/illinois-temporal/output/all_output/"
input_files = [INPUT_DIRECTORY+f for f in os.listdir(INPUT_DIRECTORY) if f[-3:] == 'txt']
print(len(input_files))
lemmatizer = WordNetLemmatizer()
for infile in input_files:
    infile = open(infile, 'r+')
    lines = infile.readlines()
    verbs = []
    for line in lines:
        line = line[:-1]
        if ':' in line:
            verb = line.split(':')[-1]
            try:
                verb = lemmatizer.lemmatize(verb, 'v')
            except Exception as e:
                pass
            verbs.append(verb)
    docs.append(verbs)
model = Word2Vec(docs, iter=100, min_count=1, sg=0)
model.save('cbow_training_235000.bin')
