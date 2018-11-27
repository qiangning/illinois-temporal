import os
from nltk.stem.wordnet import WordNetLemmatizer

# INPUT_DIRECTORY = "/scratch/sanjay/illinois-temporal/output/all_output/"
# input_files = [INPUT_DIRECTORY+f for f in os.listdir(INPUT_DIRECTORY) if f[-3:] == 'txt']
input_files = []
INPUT_DIRECTORY = "/shared/preprocessed/sssubra2/annotated-nyt-temporal/extracted"
for root, _, files in os.walk(INPUT_DIRECTORY):
    for fname in files:
        input_files.append(os.path.join(root, fname))
print(len(input_files))
lemmatizer = WordNetLemmatizer()
adj_list = {}
all_verbs = set()
for i, infile in enumerate(input_files):
    infile = open(infile, 'r+')
    lines = infile.readlines()
    verbs = []
    for line in lines:
        line = line[:-1]
        # if ':' in line:
        if len(line) > 0 and line[0] == 'E':
            """verb = line.split(':')[-1].lower()
            try:
                verb = lemmatizer.lemmatize(verb, 'v')
            except Exception as e:
                pass"""
            verb = line.split('/')[4]
            """for i, v in enumerate(verbs):
                if verb not in adj_list[v].keys():
                    adj_list[v][verb] = 0
                adj_list[v][verb] += 1.0/(len(verbs)-i)"""
            for v in verbs:
                if v not in adj_list:
                    adj_list[v] = {}
                if v > verb:
                    if verb not in adj_list:
                        adj_list[verb] = {}
                    if v not in adj_list[verb]:
                        adj_list[verb][v] = {}
                        adj_list[verb][v]['forward'] = 0
                        adj_list[verb][v]['backward'] = 0
                    adj_list[verb][v]['backward'] += 1
                elif v < verb:
                    if v not in adj_list:
                        adj_list[v] = {}
                    if verb not in adj_list[v]:
                        adj_list[v][verb] = {}
                        adj_list[v][verb]['forward'] = 0
                        adj_list[v][verb]['backward'] = 0
                    adj_list[v][verb]['forward'] += 1
            verbs.append(verb)
            all_verbs.add(verb)
            """if verb not in adj_list.keys():
                adj_list[verb] = {}
            verbs.append(verb)"""
    if i % 1000 == 0:
        print(i)
prob_file = open('probabilities_full.txt', 'w+')
for v in adj_list.keys():
    for verb in adj_list[v].keys():
        prob_file.write(v+','+verb+','+str(float(adj_list[v][verb]['forward'])/(float(adj_list[v][verb]['forward'])+float(adj_list[v][verb]['backward'])))+','+str(adj_list[v][verb]['forward']+adj_list[v][verb]['backward'])+'\n')
prob_file.close()
