import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._

abstract class Rank extends DefinedValue
object Rank {
  def apply(c: Char): Validated[ErrorMsg, Rank] = {
    c match {
      case '2' => Two.valid
      case '3' => Three.valid
      case '4' => Four.valid
      case '5' => Five.valid
      case '6' => Six.valid
      case '7' => Seven.valid
      case '8' => Eight.valid
      case '9' => Nine.valid
      case 'T' => Ten.valid
      case 'J' => Jack.valid
      case 'Q' => Queen.valid
      case 'K' => King.valid
      case 'A' => Ace.valid
      case cc => Invalid(ErrorMsg(s"$cc is not valid field for ${Rank.getClass.getName}"))
    }
  }
}

object Two   extends Rank { override def value: Int = 2; override def asString: String = value.toString }
object Three extends Rank { override def value: Int = 3; override def asString: String = value.toString }
object Four  extends Rank { override def value: Int = 4; override def asString: String = value.toString }
object Five  extends Rank { override def value: Int = 5; override def asString: String = value.toString }
object Six   extends Rank { override def value: Int = 6; override def asString: String = value.toString }
object Seven extends Rank { override def value: Int = 7; override def asString: String = value.toString }
object Eight extends Rank { override def value: Int = 8; override def asString: String = value.toString }
object Nine  extends Rank { override def value: Int = 9; override def asString: String = value.toString }
object Ten   extends Rank { override def value: Int = 10; override def asString: String = "T" }
object Jack  extends Rank { override def value: Int = 11; override def asString: String = "J" }
object Queen extends Rank { override def value: Int = 12; override def asString: String = "Q" }
object King  extends Rank { override def value: Int = 13; override def asString: String = "K" }
object Ace   extends Rank { override def value: Int = 14; override def asString: String = "A" }