package net.maxsmr.copyutil.utils.logger.holder;


public interface ILoggerHolderProvider<H extends BaseLoggerHolder> {

     H provideHolder();
}
