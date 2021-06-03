import matplotlib

matplotlib.use('Agg')
import sys

import matplotlib.pyplot as plt


def read_csv(file):
    lines = []
    with open(file) as f:
        content = f.readlines()
        for line in content:
            lines.append(line.split())
    return lines


if __name__ == '__main__':

    print(sys.argv)

    size = len(sys.argv) - 3

    for i in range(2):
        finy = [0 for _ in range(200010)]
        x1 = [i1 for i1 in range(len(finy))]
        for j in range(size // 2):
            ll = read_csv(sys.argv[1 + size // 2 * i + j])
            x = [int(i1[0]) for i1 in ll]
            y = [float(i1[1]) for i1 in ll]
            cur = x[0]
            finy[x[0]] += y[0]
            for iii in range(1, len(x)):
                for jj in range(cur + 1, x[iii] + 1):
                    finy[jj] += y[iii - 1]
                    cur = x[iii]
            for iii in range(cur, len(finy)):
                finy[iii] = finy[iii - 1]

        finy = [i / (size // 2) for i in finy]

        if (finy[0] == 0):
            iii = 0
            while (finy[iii] == 0):
                iii+=1
            val = finy[iii]
            while (iii != -1):
                finy[iii] = val
                iii-=1

        if (finy[-1] == 0):
            iii = -1
            while (finy[iii] == 0):
                iii-=1
            val = finy[iii]
            while (iii != 0):
                finy[iii] = val
                iii+=1

        plt.plot(x1, finy, label=sys.argv[i + len(sys.argv) - 2])

    #     plt.xscale('log')
    plt.xlabel('Steps')
    plt.ylabel('logLikelihood (More is better)')
    plt.legend()
    plt.savefig("plt.png")
