import matplotlib

matplotlib.use('Agg')

import sys

from sklearn.metrics import precision_recall_curve
from sklearn.metrics import auc
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


def read_csv(file):
    lines = []
    with open(file) as f:
        content = f.readlines()
        for line in content:
            lines.append(line.split())
    return lines

def get_last(name):
    idx = name.rfind('/')
    return name[idx + 1:]

if __name__ == '__main__':
    print(sys.argv)

    f = True
    if (sys.argv[-1] == "noau"):
        sys.argv = sys.argv[:-1:]
        f = False
    size = len(sys.argv) - 2

    i = 0
    while i < size:
        ress = []
        read_out_tsv = read_csv(sys.argv[size + 1])

        index = 0
        elem = {}
        for row in read_out_tsv:
            res = row[0] + " " + row[1]
            ress.append(tuple([res, int(row[2]), 0.]))
            elem[res] = index
            index += 1

        read_out_tsv = read_csv(sys.argv[i + 1])
        for row in read_out_tsv:
            res = row[0] + " " + row[1]
            if res in elem:
                tuple1 = ress[elem[res]]
                ress[elem[res]] = tuple([tuple1[0], tuple1[1], float(row[2])])
            else:
                ress.append(tuple([res, 0., float(row[2])]))
                elem[res] = index
                index += 1

        ress = sorted(ress, key=lambda a: -a[2])

        real_y = [i[2] for i in ress]
        gold_y = [i[1] for i in ress]

        precision, recall, _ = precision_recall_curve(np.array(gold_y), np.array(real_y))
        val = 0.
        try:
            val = auc(recall, precision)
            print(val)
        except ValueError:
            pass
        if (f):
            if i + 2 != size and sys.argv[i + 2] == 'as':
                plt.step(recall, precision, where='post', label=get_last(sys.argv[i + 3]) + " AUPR: " + "{:.5f}".format(val))
                i += 2
            else:
                plt.step(recall, precision, where='post', label=get_last(sys.argv[i + 1]) + " AUPR: " + "{:.5f}".format(val))
        else:
            if i + 2 != size and sys.argv[i + 2] == 'as':
                plt.step(recall, precision, where='post', label=get_last(sys.argv[i + 3]))
                i += 2
            else:
                plt.step(recall, precision, where='post', label=get_last(sys.argv[i + 1]))
        i += 1

    plt.ylim([0.0, 1.05])
    plt.xlim([0.0, 1.05])
    plt.xlabel('Recall')
    plt.ylabel('Precision')
    plt.legend()
    plt.savefig("Precision_Recall.png")
