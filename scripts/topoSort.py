from igraph import *
import numpy as np
import copy
import math
import matplotlib.pyplot as plt
import pickle as pkl

class tester:
    def __init__(self,g=None):
        self.res = []
        if g==None:
            self.num_vertices = 6
            self.g = Graph(directed=True)
            # Vertices
            names = [str(num) for num in range(self.num_vertices)]
            self.g.add_vertices(self.num_vertices)
            self.g.vs["label"] = names
            self.g.vs["name"] = self.g.vs["label"]
            self.g.add_edges([(5,2), (5,0), (4,0), (4,1), (2,3), (3,1)])
        else:
            self.g = g
            self.num_vertices = g.vcount()
            self.num_edges = g.ecount()
        self.num_topoSort = 0
        self.visited = [False for i in range(self.num_vertices)]

    def alltopoSortUtil(self):
        flag = False
        for i in range(self.num_vertices):
            if self.g.indegree(i) == 0 and not self.visited[i]:
                #reducing indegree of adjacent vertices
                current_edges = self.g.get_edgelist()
                edges_to_remove = [current_edges[eg] for eg in self.g.incident(i, mode="out")]
                self.g.delete_edges(edges_to_remove)

                # including in result
                self.res.append(i)
                self.visited[i] = True
                self.alltopoSortUtil()

                self.visited[i] = False
                self.res = self.res[0:-1]
                self.g.add_edges(edges_to_remove)
                flag = True

        if not flag:
            # print(self.res)
            self.num_topoSort += 1

    def alltopoSort(self):
        self.num_vertices = self.g.vcount()
        self.res = []
        self.alltopoSortUtil()

def permuted_edges(num_vertices,seed=0):
    edges = []
    for i in range(num_vertices):
        for j in range(i+1, num_vertices):
            edges.append((i,j))

    edges = np.random.RandomState(seed=seed).permutation(edges)
    return edges

def exp(seed=[0],num_vertices=5,num_edges=range(3,10)):
    num_topo_all = []
    for sd in seed:
        edges = permuted_edges(num_vertices,sd)
        num_topo = []
        print("Seed=%-4d" % sd)
        for i in num_edges:
            g = Graph(directed=True)
            # Vertices
            names = [str(num) for num in range(num_vertices)]
            g.add_vertices(num_vertices)
            g.vs["label"] = names
            g.vs["name"] = g.vs["label"]
            # Edges
            g.add_edges(edges[0:i+1])
            tmp = tester(g)
            tmp.alltopoSort()
            num_topo += [tmp.num_topoSort]
        num_topo_all += num_topo
    num_topo_all = np.array(num_topo_all)
    num_topo_all = num_topo_all.reshape((len(seed),len(num_edges)))
    return num_topo_all