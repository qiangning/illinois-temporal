import time
import torch
import torch.nn as nn
import torch.nn.functional as F
from sklearn.metrics import f1_score, recall_score, precision_score
from sklearn.model_selection import train_test_split
import re
import numpy as np

class VerbNet(nn.Module):
    def __init__(self, vocab_size):
        super(VerbNet, self).__init__()
        self.emb_size = 100
        self.emb_layer = nn.Embedding(vocab_size, self.emb_size)
        self.fc1 = nn.Linear(self.emb_size*2, self.emb_size)
        self.fc2 = nn.Linear(self.emb_size, self.emb_size/2)
        self.fc3 = nn.Linear(self.emb_size/2, 1)
        self.is_training = True
    def forward(self, x):
        x_emb = self.emb_layer(x)
        fullX = torch.cat((x_emb[:,0,:], x_emb[:,1,:]), dim=1)
        layer1 = F.relu(self.fc1(F.dropout(fullX, p=0.3, training=self.is_training)))
        layer2 = F.relu(self.fc2(F.dropout(layer1, p=0.3, training=self.is_training)))
        layer3 = torch.sigmoid(self.fc3(layer2))
        return layer3

class FfnnTrainer():
    def __init__(self, ffnn):
        self.ffnn = ffnn
        self.optimizer = torch.optim.Adam(ffnn.parameters(), lr=1e-4)
        self.loss = nn.BCELoss()
    def train(self, X_train, Y_train, counts_train, X_test, Y_test, counts_test):
        loss_value = np.inf
        prev_loss_value = np.inf
        count = 0
        batch_size = 1000
        train_losses = []
        test_recalls = []
        test_precisions = []
        while count < 15: # and (count < 2 or abs(loss_value-prev_loss_value) > 1):
            prev_loss_value = loss_value
            loss_value = 0
            self.ffnn.is_training = True
            start = time.time()
            for i in range(0, X_train.shape[0], batch_size):
                x = np.int64(X_train[i:min(i+batch_size, X_train.shape[0]),:])
                c = counts_train[i:min(i+batch_size, X_train.shape[0])]
                x = np.repeat(x, c, axis=0)
                y = Y_train[i:min(i+batch_size, X_train.shape[0])]
                y = np.repeat(y, c, axis=0)
                if np.random.rand() >= 0.5:
                    x[:,0], x[:,1] = x[:,1], x[:,0].copy()
                    y = 1.0-y
                y_pred = self.ffnn(torch.from_numpy(x).cuda())
                L = self.loss(y_pred.cuda().float(), torch.from_numpy(y).cuda().float())
                loss_value += L.item()
                self.optimizer.zero_grad()
                L.backward()
                self.optimizer.step()
            end = time.time()
            print(count, loss_value, 'time', end-start)
            train_losses.append(loss_value)
            if count % 1 == 0:
                self.ffnn.is_training = False
                y_true = []
                y_pred = []
                start = time.time()
                for i in range(0, X_test.shape[0], batch_size):
                    x = np.int64(X_test[i:min(i+batch_size, X_test.shape[0]),:])
                    c = counts_test[i:min(i+batch_size, X_test.shape[0])]
                    x = np.repeat(x, c, axis=0)
                    y = Y_test[i:min(i+batch_size, X_test.shape[0])]
                    y = np.repeat(y, c, axis=0)
                    if np.random.rand() >= 0.5:
                        x[:,0], x[:,1] = x[:,1], x[:,0].copy()
                        y = 1.0-y
                    y_true += list(np.int32(y >= 0.5))
                    y_pre = self.ffnn(torch.from_numpy(x).cuda()).cpu().detach().numpy()
                    y_pred += list(np.int32(y_pre >= 0.5))
                end = time.time()
                recall = recall_score(y_true, y_pred)
                precision = precision_score(y_true, y_pred)
                f1 = f1_score(y_true, y_pred)
                test_recalls.append(recall)
                test_precisions.append(precision)
                print(count, precision, recall, f1, 'time', end-start)
            count += 1
        train_losses = np.array(train_losses)
        test_recalls = np.array(test_recalls)
        test_precisions = np.array(test_precisions)
        np.save('/scratch/sanjay/illinois-temporal/embeddings/train_losses_dist2.npy', train_losses)
        np.save('/scratch/sanjay/illinois-temporal/embeddings/test_recalls_dist2.npy', test_recalls)
        np.save('/scratch/sanjay/illinois-temporal/embeddings/test_precisions_dist2.npy', test_precisions)
        torch.save({'epoch': count,
                    'model_state_dict': self.ffnn.state_dict(),
                    'optimizer_state_dict': self.optimizer.state_dict(),
                    'loss': loss_value}, '/scratch/sanjay/illinois-temporal/embeddings/pairwise_model_dist2.pt')

if __name__ == '__main__':
    all_verbs = set()
    probability_file = open('/scratch/sanjay/illinois-temporal/embeddings/probabilities_dist2.txt')
    lines = probability_file.readlines()
    train_size = 0
    regex = re.compile('[^a-zA-Z]')
    for line in lines:
        parts = line.split(',')
        if len(parts) > 4:
            continue
        all_verbs.add(regex.sub('', parts[0]))
        all_verbs.add(regex.sub('', parts[1]))
        train_size += int(parts[3])
    print(len(lines), len(all_verbs))
    print(train_size)
    X = np.zeros((len(lines), 2))
    counts = np.zeros((len(lines)), dtype=np.int32)
    Y = np.zeros((len(lines), 1))
    all_verbs = sorted(list(all_verbs))
    verb_i_map = {}
    for i, verb in enumerate(all_verbs):
        verb_i_map[verb] = i
    for i, line in enumerate(lines):
        parts = line.split(',')
        if len(parts) > 4:
            continue
        X[i,0] = verb_i_map[regex.sub('', parts[0])]
        X[i,1] = verb_i_map[regex.sub('', parts[1])]
        counts[i] = int(parts[3])
        Y[i] = float(parts[2])
    X_train, X_test, Y_train, Y_test, counts_train, counts_test = train_test_split(X, Y, counts, test_size=0.2)
    ffnn = VerbNet(len(all_verbs))
    ffnn.cuda()
    trainer = FfnnTrainer(ffnn)
    trainer.train(X_train, Y_train, counts_train, X_test, Y_test, counts_test)
    ffnn.is_training = False
    y_true = []
    y_pred = []
    batch_size = 1000
    for i in range(0, X_test.shape[0], batch_size):
        x = np.int64(X_test[i:min(i+batch_size, X_test.shape[0]),:])
        c = counts_test[i:min(i+batch_size, X_test.shape[0])]
        x = np.repeat(x, c, axis=0)
        y = Y_test[i:min(i+batch_size, X_test.shape[0])]
        y = np.repeat(y, c, axis=0)
        y_true += list(np.int32(y >= 0.5))
        y_pre = ffnn(torch.from_numpy(x).cuda())
        y_pred += list(np.int32(y_pre >= 0.5))
    recall = recall_score(y_true, y_pred)
    precision = precision_score(y_true, y_pred)
    f1 = f1_score(y_true, y_pred)
    print('final', precision, recall, f1)
