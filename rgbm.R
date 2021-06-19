options(stringsAsFactors=FALSE)
library(RGBM)
args = commandArgs(trailingOnly=TRUE)
filename <- args[1]
df <- data.table::fread(filename, data.table = F)

tmp = dim(df)
x = tmp[1]
y = tmp[2]
m <- as.matrix(df)
K <- matrix(0, x, y)
colnames(K) <- colnames(m)
library(doParallel)
registerDoParallel(cores=32)
cl <- makeCluster(32)
res <- RGBM(E = m, K, g_M = matrix(1, 195, y), tfs = paste0("G", c(1:195)),
            targets = paste0("G", 1:y))
stopCluster(cl)
warnings()
write.table(res,file="test.txt")
system(paste("python3 RGBM.py >", args[2], sep=" "))