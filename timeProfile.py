import matplotlib

matplotlib.use('Agg')
import sys

import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

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
    index = 0
    for i in sys.argv:
        if i == 'break':
            break
        index+=1

    ys = []
    xs = []
    run = []
    algo = []
    for i in range(index - 1):
        ll = read_csv(sys.argv[1 + i])
        x = [(int(i1[0]) + 9999) // 10000 * 10000 for i1 in ll]
        y = [int(i1[1]) // 1000 for i1 in ll]
        x = x[::10]
        y = y[::10]
        ys += y
        xs += x
        run += [i for i in range(len(x))]
        algo += [sys.argv[-2] for i in range(len(x))]

    for i in range(index, size):
        ll = read_csv(sys.argv[1 + i])
        x = [(int(i1[0]) + 9999) // 10000 * 10000 for i1 in ll]
        y = [int(i1[1]) // 1000 for i1 in ll]
        x = x[::10]
        y = y[::10]
        ys += y
        xs += x
        run += [i for i in range(len(x))]
        algo += [sys.argv[-1] for i in range(len(x))]

    d = {'step': xs, 'time': ys, 'run' : run, 'algo' : algo}
    print(len(xs), len(ys), len(run))
    df = pd.DataFrame(data=d)
    print(df.head())

    sns.set_theme(style="darkgrid")
    myplot = sns.lineplot(x = "step", y = "time" , data = df, hue = 'algo')
    myplot.set_yscale("log")

    myplot.set(xlabel="Steps", ylabel = "Time, seconds (Less is better)")
#     myplot.set_yticklabels(myplot.get_yticks(), size = 5)
#     plt.yticks(fontsize=8, rotation=45)
#     plt.legend()
    myplot.figure.savefig("speed.png")
