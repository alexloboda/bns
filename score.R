library(bnlearn)
library(stats)

server_up <- function(ge_file, sf, port, bs = NULL) {
  sockd <- socketConnection(port = port, server = TRUE)
  on.exit(close(sockd))
  ge <- read.table(ge_file, header = TRUE, sep = "\t", stringsAsFactors = FALSE)
  
  obs_num <- nrow(ge)
  ideal_normal <- qnorm(seq(0, 1, length.out = obs_num + 2)[1:obs_num + 1])
  if (sf == "bde") {
    ge[] <- lapply(ge, function(x) {
      breaks <- seq(0, 1, length.out = bs + 1)
      breaks <- breaks[2:(length(breaks) - 1)]
      breaks <- ceiling(breaks * obs_num)
      breaks <- x[order(x)][breaks]
      as.factor(findInterval(x, breaks) + 1)
    })
  } else {
    ge[] <- sapply(ge, function(x) {
        ideal_normal[order(order(x))]
    })  
  }
  
  e <- empty.graph(names(ge))
  while (TRUE) {
    l <- readLines(sockd, n = 1)
    genes <- strsplit(l, "\t")[[1]]
    target <- genes[1]
    if(length(genes) > 1) {
      origins <- genes[2:length(genes)]
      bn_arcs <- Reduce(function(x, y) rbind(x, c(y, target)), origins, integer(0))
    } else {
      bn_arcs <- matrix(integer(0), nrow = 0, ncol = 2)
    }
    colnames(bn_arcs) <- c("from", "to")
    arcs(e) <- bn_arcs
    writeLines(as.character(score(e, ge, type = sf, by.node = TRUE)[target]), 
               con = sockd)
  } 
}
