import matplotlib

matplotlib.use('Agg')

def strip(string):
    if (string[0] == "\"" or string[0] == "\'"):
        string = string[1:]
    if (string[-1] == "\"" or string[-1] == "\'"):
        string = string[:-1]
    return string
        

def read_csv(file):
    lines = []
    with open(file) as f:
        content = f.readlines()
        for line in content:
            lines.append(line.split())
    return lines

if __name__ == '__main__':
    result = read_csv("test.txt")
    out_res = []
    for i in range(1, len(result)):
        for j in range(1, len(result[i])):
            out_res.append((result[i][0], result[0][j - 1], result[i][j]))
    out_res = sorted(out_res, key = lambda x: -float(x[2]))
    for i in out_res:
        print(strip(i[0]), "\t", strip(i[1]), "\t",i[2], "\t", sep="")

