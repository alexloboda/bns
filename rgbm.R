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

res_df <- data.frame(from = c(), to = c(), score = c())
for (i in 1:nrow(res)) {
  for (j in 1:ncol(res)) {
    if (i != j) {
       res_df <- rbind(res_df, list(from = rownames(res)[i], to = colnames(res)[j],
                       score = res[i, j]))
    }
  }
}
res_df <- res_df[order(res_df[, 3], decreasing = TRUE), ]
data.table::fwrite(res_df, args[2], col.names = FALSE, sep = "\t")
warnings()