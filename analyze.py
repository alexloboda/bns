import csv
import sys

from sklearn.metrics import precision_recall_curve
from sklearn.metrics import plot_precision_recall_curve
import matplotlib.pyplot as plt
import numpy as np


class classifier:
    def __init__(self, ans):
        self._estimator_type = "classifier"
        self.decision_function = self.fit
        self.ans = ans
        self.classes_ = [0., 1.]

    def fit(self, arr):
        res = []
        for j in arr:
            resAc = False
            for i in self.ans:
                if i[0][0] == j[0] and i[0][1] == j[1]:
                    res.append(1.)
                    resAc = True
                    continue
            if not resAc:
                res.append(0.)
        return np.array(res)


if __name__ == '__main__':
    print(sys.argv)
    ress = []

    tsv_out_file = open(sys.argv[2])
    read_out_tsv = csv.reader(tsv_out_file, delimiter="\t")

    index = 0
    elem = {}
    for row in read_out_tsv:
        res = row[0] + " " + row[1]
        ress.append(tuple([res, int(row[2]), 0.]))
        elem[res] = index
        index += 1

    tsv_out_file = open(sys.argv[1])
    read_out_tsv = csv.reader(tsv_out_file, delimiter="\t")
    for row in read_out_tsv:
        res = row[0] + " " + row[1]
        tuple1 = ress[elem[res]]
        ress[elem[res]] = tuple([tuple1[0], tuple1[1], float(row[2])])

    ress = sorted(ress, key = lambda a: -a[2])

    real_y = [i[2] for i in ress ]
    gold_y = [i[1] for i in ress ]

    precision, recall, _ = precision_recall_curve(np.array(gold_y), np.array(real_y))

    plt.figure()
    plt.step(recall, precision, where='post')

    plt.ylim([-0.01, 1.05])
    plt.xlim([-0.01, 1.05])
    plt.xlabel('Recall')
    plt.ylabel('Precision')
    plt.savefig("Precision_Recall.png")
