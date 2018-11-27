import os
from nltk.stem.wordnet import WordNetLemmatizer

INPUT_DIRECTORY = "/scratch/sanjay/illinois-temporal/output/all_output/"
input_files = [INPUT_DIRECTORY+f for f in os.listdir(INPUT_DIRECTORY) if f[-3:] == 'txt']
print(len(input_files))
lemmatizer = WordNetLemmatizer()
adj_list = {}
all_verbs = set()
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
            """for i, v in enumerate(verbs):
                if verb not in adj_list[v].keys():
                    adj_list[v][verb] = 0
                adj_list[v][verb] += 1.0/(len(verbs)-i)"""
            all_verbs.add(verb)
            """if verb not in adj_list.keys():
                adj_list[verb] = {}
            verbs.append(verb)"""
all_verbs = sorted(list(all_verbs))
# node2vec_infile = open('/scratch/sanjay/illinois-temporal/embeddings/node2vec_235000.edgelist', 'w+')
verb_id_map = {}
"""for i, verb in enumerate(all_verbs):
    verb_id_map[verb] = i"""
emd_infile = open('/scratch/sanjay/illinois-temporal/embeddings/node2vec_235000.emd')
lines = emd_infile.readlines()
emd_outfile = open('/scratch/sanjay/illinois-temporal/embeddings/node2vec_235000.embed', 'w+')
for line in lines:
    parts = line.split()
    parts[0] = int(parts[0])
    parts[1:] = [float(part) for part in parts[1:]]
    emd_outfile.write(all_verbs[parts[0]])
    for num in parts[1:]:
        emd_outfile.write(' '+str(num))
    emd_outfile.write('\n')
emd_outfile.close()
