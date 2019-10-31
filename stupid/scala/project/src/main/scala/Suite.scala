import cats.data.Validated
import cats.data.Validated.Invalid
import cats.implicits._

abstract class Suite extends DefinedValue
object Suite {
  def apply(c: Char): Validated[ErrorMsg, Suite] = {
    c match {
      case 'H' => Hearths.valid
      case 'D' => Diamonds.valid
      case 'C' => Clubs.valid
      case 'S' => Spades.valid
      case cc => Invalid(ErrorMsg(s"$cc is not valid field for ${Suite.getClass.getName}"))
    }
  }
}

object Hearths  extends Suite { override def value: Int = 1; override def asString: String = "♥" }
object Diamonds extends Suite { override def value: Int = 2; override def asString: String = "♦" }
object Clubs    extends Suite { override def value: Int = 3; override def asString: String = "♣" }
object Spades   extends Suite { override def value: Int = 4; override def asString: String = "♠" }