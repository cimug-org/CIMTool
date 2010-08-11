package au.com.langdale.eclipse

trait DisposeChain {
  def disposeChain: Unit
}

trait Disposable extends DisposeChain {
  def disposeChain {}
}
