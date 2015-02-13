/**
 * Created by mentall on 13.02.15.
 */
trait MyBeautifulOutput{
  val maxLength = 46

  def out(s : String) = {
    val marksCount = (maxLength - s.length) / 2
    if (s.length % 2 == 0) {
      println("-" * marksCount + s.toUpperCase + "-" * marksCount)
    }
    else{
      println("-" * marksCount + s.toUpperCase + "-" * (marksCount - 1))
    }
  }
}
