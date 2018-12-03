import torch
import torch.optim as optim
from bilstm_crf import BiLSTM_CRF
import xml.etree.ElementTree as ET
from allennlp.commands.elmo import ElmoEmbedder
from sklearn.metrics import f1_score, recall_score, precision_score

START_TAG = "<START>"
STOP_TAG = "<STOP>"
EMBEDDING_DIM = 1024
HIDDEN_DIM = 100

# options_file = "https://s3-us-west-2.amazonaws.com/allennlp/models/elmo/2x4096_512_2048cnn_2xhighway/elmo_2x4096_512_2048cnn_2xhighway_options.json"
# weight_file = "https://s3-us-west-2.amazonaws.com/allennlp/models/elmo/2x4096_512_2048cnn_2xhighway/elmo_2x4096_512_2048cnn_2xhighway_weights.hdf5"
elmo = ElmoEmbedder()

# training_data = []
tags = []
sentences = []
tags_test = []
sentences_test = []
training_file = "/shared/preprocessed/sssubra2/trainset-events.xml"
test_file  = "/shared/preprocessed/sssubra2/testset-events.xml"

tree = ET.parse(training_file)
root = tree.getroot()
tag_to_ix = {"O": 0, "E": 1, START_TAG: 2, STOP_TAG: 3}
for element in root:
    parts = element.text.strip().split()
    sentence = []
    tag_seq = []
    for part in parts:
        subparts = part.split('///')
        sentence.append(subparts[0])
        tag_seq.append(tag_to_ix[subparts[-1]])
    # training_data.append((sentence, tag_seq))
    sentences.append(sentence)
    tags.append(tag_seq)

print("Processed training data")

tree = ET.parse(test_file)
root = tree.getroot()
for element in root:
    parts = element.text.strip().split()
    sentence = []
    tag_seq = []
    for part in parts:
        subparts = part.split('///')
        sentence.append(subparts[0])
        tag_seq.append(tag_to_ix[subparts[-1]])
    # training_data.append((sentence, tag_seq))
    sentences_test.append(sentence)
    tags_test.append(tag_seq)

print("Processed test data")

embeddings = list(elmo.embed_sentences(sentences))
embeddings_test = list(elmo.embed_sentences(sentences_test))
print("Loaded embeddings")

model = BiLSTM_CRF(tag_to_ix, EMBEDDING_DIM, HIDDEN_DIM, tag_to_ix[START_TAG], tag_to_ix[STOP_TAG])
model = model.cuda()
optimizer = optim.SGD(model.parameters(), lr=0.01, weight_decay=1e-4)

# Check predictions before training
"""with torch.no_grad():
    precheck_sent = prepare_sequence(training_data[0][0], word_to_ix)
    precheck_tags = torch.tensor([tag_to_ix[t] for t in training_data[0][1]], dtype=torch.long)
    print(model(precheck_sent))"""

training_losses = []
test_recalls = []
test_precisions = []
# Make sure prepare_sequence from earlier in the LSTM section is loaded
for epoch in range(1, 21):  # again, normally you would NOT do 300 epochs, it is toy data
    total_loss = 0
    for embeds, tag_seq in zip(embeddings, tags):
        # Step 1. Remember that Pytorch accumulates gradients.
        # We need to clear them out before each instance
        model.zero_grad()

        # Step 2. Get our inputs ready for the network, that is,
        # turn them into Tensors of word indices.
        # sentence_in = prepare_sequence(sentence, word_to_ix)
        targets = torch.tensor(tag_seq, dtype=torch.long).cuda()

        # Step 3. Run our forward pass.
        loss = model.neg_log_likelihood(torch.from_numpy(embeds[0]).cuda(), targets)
        total_loss += loss.item()

        # Step 4. Compute the loss, gradients, and update the parameters by
        # calling optimizer.step()
        loss.backward()
        optimizer.step()
    print('epoch', epoch, total_loss)
    target = []
    pred = []
    for embeds, tag_seq in zip(embeddings_test, tags_test):
        _, pred_tags = model(torch.from_numpy(embeds).cuda())
        target += tag_seq
        pred += pred_tags
    test_recall = recall_score(target, pred)
    test_precision = precision_score(target, pred)
    test_f1 = f1_score(target, pred)
    test_recalls.append(test_recall)
    test_precisions.append(test_precision)
    print('test f1', test_f1)
    np.save('/scratch/sanjay/illinois-temporal/embeddings/bilstm_crf_train_losses.npy', train_losses)
    np.save('/scratch/sanjay/illinois-temporal/embeddings/bilstm_crf_test_recalls.npy', test_recalls)
    np.save('/scratch/sanjay/illinois-temporal/embeddings/bilstm_crf_test_precisions.npy', test_precisions)
    if epoch % 5 == 0:
        torch.save({'epoch': epoch,
                    'model_state_dict': model.state_dict(),
                    'optimizer_state_dict': optimizer.state_dict(),
                    'loss': total_loss}, '/scratch/sanjay/illinois-temporal/embeddings/bilstm_crf_model.pt')
