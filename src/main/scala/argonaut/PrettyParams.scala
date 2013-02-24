package argonaut

import scalaz._, Scalaz._
import annotation.tailrec

/**
 * Parameters for pretty-printing a JSON value.
 *
 * @author Tony Morris
 */
sealed trait PrettyParams {
  /**
   * Takes the current depth and returns the spaces to insert to left of a left brace.
   */
  val lbraceLeft: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to right of a left brace.
   */
  val lbraceRight: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to left of a right brace.
   */
  val rbraceLeft: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to right of a right brace.
   */
  val rbraceRight: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to left of a left bracket.
   */
  val lbracketLeft: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to right of a left bracket.
   */
  val lbracketRight: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to left of a right bracket.
   */
  val rbracketLeft: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to right of a right bracket.
   */
  val rbracketRight: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to left of a comma.
   */
  val commaLeft: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to right of a comma.
   */
  val commaRight: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to left of a colon.
   */
  val colonLeft: Int => String

  /**
   * Takes the current depth and returns the spaces to insert to right of a colon.
   */
  val colonRight: Int => String

  private[this] final val openBraceText = "{"
  private[this] final val closeBraceText = "}"
  private[this] final val openArrayText = "["
  private[this] final val closeArrayText = "]"
  private[this] final val commaText = ","
  private[this] final val colonText = ":"
  private[this] final val nullText = "null"
  private[this] final val trueText = "true"
  private[this] final val falseText = "false"
  private[this] final val stringEnclosureText = "\""

  /**
   * Returns a string representation of a pretty-printed JSON value.
   */
  def pretty(j: Json): String = {
    import Json._
    import StringEscaping._
    def encloseJsonString(builder: StringBuilder, jsonString: JsonString): StringBuilder = {
      // TODO: Improve this so that it doesn't need to call escape for every single char.
      jsonString.foldLeft(builder.append(stringEnclosureText))((working, char) => working.append(escape(char))).append(stringEnclosureText)
    }
    def trav(builder: StringBuilder, depth: Int, k: Json): StringBuilder = {
      def lbrace(builder: StringBuilder): StringBuilder = {
        builder.append(lbraceLeft(depth)).append(openBraceText).append(lbraceRight(depth + 1))
      }
      def rbrace(builder: StringBuilder): StringBuilder = {
        builder.append(rbraceLeft(depth + 1)).append(closeBraceText).append(rbraceRight(depth))
      }
      def lbracket(builder: StringBuilder): StringBuilder = {
        builder.append(lbracketLeft(depth)).append(openArrayText).append(lbracketRight(depth + 1))
      }
      def rbracket(builder: StringBuilder): StringBuilder = {
        builder.append(rbracketLeft(depth + 1)).append(closeArrayText).append(rbracketRight(depth))
      }
      def comma(builder: StringBuilder): StringBuilder = {
        builder.append(commaLeft(depth + 1)).append(commaText).append(commaRight(depth + 1))
      }
      def colon(builder: StringBuilder): StringBuilder = {
        builder.append(colonLeft(depth + 1)).append(colonText).append(colonRight(depth + 1))
      }

      k.fold[StringBuilder](
        builder.append(nullText)
        , bool => builder.append(if (bool) trueText else falseText)
        , n => builder.append(n.shows)
        , s => encloseJsonString(builder, s)
        , e => {
          e.foldLeft((true, lbracket(builder))){case ((firstElement, builder), subElement) =>
            val withComma = if(firstElement) builder else comma(builder)
            val updatedBuilder = trav(withComma, depth + 1, subElement)
            (false, updatedBuilder)
          }._2 |> rbracket
        }
        , o => {
          o.toList.foldLeft((true, lbrace(builder))){case ((firstElement, builder), (key, value)) =>
            val withComma = if(firstElement) builder else comma(builder)
            val updatedBuilder = trav(encloseJsonString(withComma, key) |> colon, depth + 1, value)
            (false, updatedBuilder)
          }._2 |> rbrace
        }
      )
    }

    trav(new StringBuilder(), 0, j).toString()
  }

  /**
   * Returns a `Vector[Char]` representation of a pretty-printed JSON value.
   */
  def lpretty(j: Json): Vector[Char] = Vector.empty[Char] ++ pretty(j)
}

object StringEscaping {
  def escape(c: Char): String =
    c match {
      case '\\' => "\\\\"
      case '"' => "\\\""
      case '\b' => "\\b"
      case '\f' => "\\f"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case possibleUnicode if Character.isISOControl(possibleUnicode) => "\\u%04x".format(possibleUnicode.toInt)
      case _ => c.toString
    }
}

object PrettyParams extends PrettyParamss {
  def apply(
             lbraceLeft0: Int => String
           , lbraceRight0: Int => String
           , rbraceLeft0: Int => String
           , rbraceRight0: Int => String
           , lbracketLeft0: Int => String
           , lbracketRight0: Int => String
           , rbracketLeft0: Int => String
           , rbracketRight0: Int => String
           , commaLeft0: Int => String
           , commaRight0: Int => String
           , colonLeft0: Int => String
           , colonRight0: Int => String
           ): PrettyParams =
    new PrettyParams {
      val lbraceLeft = lbraceLeft0
      val lbraceRight = lbraceRight0
      val rbraceLeft = rbraceLeft0
      val rbraceRight = rbraceRight0
      val lbracketLeft = lbracketLeft0
      val lbracketRight = lbracketRight0
      val rbracketLeft = rbracketLeft0
      val rbracketRight = rbracketRight0
      val commaLeft = commaLeft0
      val commaRight = commaRight0
      val colonLeft = colonLeft0
      val colonRight = colonRight0
    }
}

trait PrettyParamss {
  val zeroString = (_: Int) => ""
  /**
   * A pretty-printer configuration that inserts no spaces.
   */
  def nospace: PrettyParams =
    PrettyParams(
      zeroString
    , zeroString
    , zeroString
    , zeroString
    , zeroString
    , zeroString
    , zeroString
    , zeroString
    , zeroString
    , zeroString
    , zeroString
    , zeroString
    )

  @tailrec
  final def repeatAppend(cord: Cord, toAppend: String, n: Int): Cord = {
    if (n > 0) repeatAppend(cord :+ toAppend, toAppend, n - 1) else cord
  }

  /**
   * A pretty-printer configuration that indents by the given spaces.
   */
  def pretty(indent: String): PrettyParams =
    PrettyParams(
      zeroString
    , n => "\n" + indent * n
    , n => "\n" + indent * (n - 1)
    , zeroString
    , zeroString
    , n => "\n" + indent * n
    , n => "\n" + indent * (n - 1)
    , zeroString
    , zeroString
    , n => "\n" + indent * n
    , n => " "
    , n => " "
    )

  /**
   * A pretty-printer configuration that indents by two spaces.
   */
  def spaces2: PrettyParams =
    pretty("  ")

  /**
   * A pretty-printer configuration that indents by four spaces.
   */
  def spaces4: PrettyParams =
    pretty("    ")
}