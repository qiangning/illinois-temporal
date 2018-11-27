import tensorflow as tf
import numpy as np
from sklearn.metrics import f1_score
from sklearn.model_selection import train_test_split

all_verbs = set()
probability_file = open('/scratch/sanjay/illinois-temporal/embeddings/probabilities.txt')
lines = probability_file.readlines()
train_size = 0
for line in lines:
    parts = line.split(',')
    all_verbs.add(parts[0])
    all_verbs.add(parts[1])
    train_size += int(parts[3])
dim = 100
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
    X[i,0] = verb_i_map[parts[0]]
    X[i,1] = verb_i_map[parts[1]]
    counts[i] = int(parts[3])
    Y[i] = float(parts[2])
X_train, X_test, Y_train, Y_test, counts_train, counts_test = train_test_split(X, Y, counts, test_size=0.2)
W = tf.Variable(tf.random_normal(shape=(len(all_verbs), dim)))
X1_tensor = tf.sparse_placeholder(dtype=tf.float32, shape=(None, len(all_verbs)))
X2_tensor = tf.sparse_placeholder(dtype=tf.float32, shape=(None, len(all_verbs)))
dropout_prob = tf.placeholder(dtype=tf.float32)
Y_tensor = tf.placeholder(dtype=tf.float32, shape=(None, 2))
# C = tf.Variable(tf.random_normal(shape=(dim, 2)))
W1 = tf.Variable(tf.random_normal(shape=(2*dim, dim)))
X1_post_emb = tf.sparse_tensor_dense_matmul(X1_tensor, W)
X2_post_emb = tf.sparse_tensor_dense_matmul(X2_tensor, W)
fullX = tf.concat([X1_post_emb, X2_post_emb], axis=1)
layer1 = tf.nn.relu(tf.matmul(tf.nn.dropout(fullX, keep_prob=dropout_prob), W1))
W2 = tf.Variable(tf.random_normal(shape=(dim, dim/2)))
layer2 = tf.nn.relu(tf.matmul(tf.nn.dropout(layer1, keep_prob=dropout_prob), W2))
Wout = tf.Variable(tf.random_normal(shape=(dim/2, 1)))
Y_pred = tf.nn.sigmoid(tf.matmul(layer2, Wout))
# Y_pred = tf.nn.softmax(tf.nn.dropout(tf.matmul(tf.sparse_tensor_dense_matmul(X1_tensor, W)-tf.sparse_tensor_dense_matmul(X2_tensor, W), C), keep_prob=dropout_prob))
# loss = -tf.reduce_sum(tf.multiply(Y_tensor, tf.log(Y_pred)))+tf.reduce_sum(tf.multiply(1-Y_tensor, tf.log(1-Y_pred)))
loss = tf.reduce_sum(tf.nn.softmax_cross_entropy_with_logits(labels=Y_tensor, logits=Y_pred))
train_op = tf.train.AdamOptimizer().minimize(loss)
loss_value = np.inf
emb_file = open('emb.txt', 'w+')
train_losses = []
test_f1s = []
test_file = open('test_pairs.txt', 'w+')
for i in range(len(X_test)):
    test_file.write(str(all_verbs[int(X_test[i,0])])+','+str(all_verbs[int(X_test[i,1])])+','+str(Y_test[i])+','+str(counts_test[i])+'\n')
test_file.close()
saver = tf.train.Saver()
with tf.Session() as sess:
    sess.run(tf.global_variables_initializer())
    # while loss_value > 18800000:
    epochs = 0
    batch_size = 500
    while epochs < 50 and (len(train_losses) < 2 or abs(train_losses[-2]-train_losses[-1]) >= 100000):
        loss_value = 0
        for i in range(int(np.ceil(X_train.shape[0]/batch_size))):
            x1 = X_train[i:min(i+batch_size, X.shape[0]),0]
            x2 = X_train[i:min(i+batch_size, X.shape[0]),1]
            c = counts_train[i:min(i+batch_size, X_train.shape[0])]
            y = Y_train[i:min(i+batch_size, X_train.shape[0]),:]
            x1 = np.repeat(x1, c, axis=0)
            indices1 = [[j, x1[j]] for j in range(len(x1))]
            x2 = np.repeat(x2, c, axis=0)
            indices2 = [[j, x2[j]] for j in range(len(x2))]
            y = np.repeat(y, c, axis=0)
            y_1minus = 1-y
            y = np.hstack([y, y_1minus])
            _, loss_v = sess.run([train_op, loss], feed_dict={X1_tensor: tf.SparseTensorValue(indices=indices1, values=[1]*len(x1), dense_shape=(len(x1), len(all_verbs))), X2_tensor: tf.SparseTensorValue(indices=indices2, values=[1]*len(x2), dense_shape=(len(x2), len(all_verbs))), Y_tensor: y, dropout_prob: 0.7})
            loss_value += loss_v
        train_losses.append(loss_value)
        epochs += 1
        print(epochs, loss_value)
        if epochs % 10 != 0:
            continue
        # Evaluation
        y_true = []
        y_pred = []
        for i in range(int(np.ceil(X_test.shape[0]/batch_size))):
            x1 = X_test[i:min(i+batch_size, X_test.shape[0]), 0]
            x2 = X_test[i:min(i+batch_size, X_test.shape[0]), 1]
            c = counts_test[i:min(i+batch_size, X_test.shape[0])]
            y = Y_test[i:min(i+batch_size, X_test.shape[0]),:]
            x1 = np.repeat(x1, c, axis=0)
            indices1 = [[j, x1[j]] for j in range(len(x1))]
            x2 = np.repeat(x2, c, axis=0)
            indices2 = [[j, x2[j]] for j in range(len(x2))]
            y = np.repeat(y, c, axis=0)
            y_pre = sess.run(Y_pred, feed_dict={X1_tensor: tf.SparseTensorValue(indices=indices1, values=[1]*len(x1), dense_shape=(len(x1), len(all_verbs))), X2_tensor: tf.SparseTensorValue(indices=indices2, values=[1]*len(x2), dense_shape=(len(x2), len(all_verbs))), dropout_prob: 1.0})
            y_true += list(np.int32(y >= 0.5))
            y_pred += list(np.int32(y_pre[:,0] >= 0.5))
        test_f1s.append(f1_score(y_true, y_pred))
    train_losses = np.array(train_losses)
    test_f1s = np.array(test_f1s)
    np.save('/scratch/sanjay/illinois-temporal/embeddings/train_losses.npy', train_losses)
    np.save('/scratch/sanjay/illinois-temporal/embeddings/test_f1s.npy', test_f1s)
    W_np = sess.run(W)
    for i in range(len(all_verbs)):
        emb_file.write(all_verbs[i] + ' ' + ' '.join([str(x) for x in W_np[i,:]]) + '\n')
    emb_file.close()
    saver.save(sess, '/scratch/sanjay/illinois-temporal/embeddings/pairwise_model.ckpt')
