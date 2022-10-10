# BNS

A tool for inferring gene regulatory networks by expression data. 

### Build 

```bash
mvn install -DskipTests
```

### Run
```bash
java -Xmx8G -jar target/grn_inferer.jar -w 100000 -s 1 -r 20 -g ge.tsv  -o out -m 1
```

Expression data is provided as a single tab separated file. Columns correspond to genes while rows represent different unnamed experiments.
An example of ge.tsv file:

| APOE | CTF1 | EGR1 | MAPK8 | POLB |
|------|------|------|-------|------|
|0.816 |0.258 |0.442 |0.178  |0.646 |
|0.558 |0.714 |0.347 |0.802  |0.852 |
|0.440 |0.572 |0.813 |0.729  |0.844 |
|0.022 |0.833 |0.580 |0.690  |0.780 |
|0.860 |0.864 |0.901 |0.867  |0.545 |

Currently, sipmle discretization is applied to values, so the absolute values are not taken into account, 
only order of values for each gene is important.

See help for more information.

```bash
java -jar target/grn_inferer.jar -h
```
