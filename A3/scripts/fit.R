df <- read.csv("7.csv", header=TRUE)
#install.packages("fitdistrplus")
library("fitdistrplus")

plotdist(df$X, histo=TRUE, demp=TRUE)
plot(density(df$X),main="Density Estimate")

plot(df$X)

d <- head(df,75500)

descdist(df$X)

par(mfrow=c(2,2))

fe.exp <- fitdist(df$X, "exp")
fe.exp$aic

fe.norm <- fitdist(df$X,"norm")
fe.norm$aic


fit.weibull <- fitdist(df$X, "weibull")
fit.gamma <- fitdist(df$X, "gamma")