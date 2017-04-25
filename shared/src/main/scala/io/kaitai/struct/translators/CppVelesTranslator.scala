package io.kaitai.struct.translators

import io.kaitai.struct.Utils
import io.kaitai.struct.exprlang.Ast
import io.kaitai.struct.exprlang.Ast.expr
import io.kaitai.struct.exprlang.DataType._
import io.kaitai.struct.languages.CppVelesCompiler

class CppVelesTranslator(provider: TypeProvider) extends BaseTranslator(provider) {
  // TODO: add string escaping
  override def doStringLiteral(s: String): String = "std::string(\"" + s + "\")"

  override def doArrayLiteral(t: BaseType, values: Seq[expr]): String =
    throw new RuntimeException("C++ literal arrays are not implemented yet")

  override def doByteArrayLiteral(arr: Seq[Byte]): String =
    "std::string(\"" + Utils.hexEscapeByteArray(arr) + "\", " + arr.length + ")"

  override def numericBinOp(left: Ast.expr, op: Ast.operator, right: Ast.expr) = {
    (detectType(left), detectType(right), op) match {
      case (_: IntType, _: IntType, Ast.operator.Mod) =>
        s"${CppVelesCompiler.kstreamName}::mod(${translate(left)}, ${translate(right)})"
      case _ =>
        super.numericBinOp(left, op, right)
    }
  }

  override def userTypeField(value: expr, attrName: String): String =
    s"${translate(value)}->${doName(attrName)}"

  override def doName(s: String) = s match {
    case "_" => s
    case _ => s"$s()"
  }

  override def doEnumByLabel(enumType: List[String], label: String): String =
    (enumType.last + "_" + label).toUpperCase
  override def doEnumById(enumType: List[String], id: String): String =
    s"static_cast<${CppVelesCompiler.type2class(enumType)}>($id)"

  override def doStrCompareOp(left: Ast.expr, op: Ast.cmpop, right: Ast.expr) = {
    if (op == Ast.cmpop.Eq) {
      s"${translate(left)} == (${translate(right)})"
    } else if (op == Ast.cmpop.NotEq) {
      s"${translate(left)} != ${translate(right)}"
    } else {
      s"(${translate(left)}.compare(${translate(right)}) ${cmpOp(op)} 0)"
    }
  }

  override def doSubscript(container: expr, idx: expr): String =
    s"${translate(container)}->at(${translate(idx)})"
  override def doIfExp(condition: expr, ifTrue: expr, ifFalse: expr): String =
    s"(${translate(condition)}) ? (${translate(ifTrue)}) : (${translate(ifFalse)})"

  // Predefined methods of various types
  override def strToInt(s: expr, base: expr): String = {
    val baseStr = translate(base)
    s"std::stoi(${translate(s)}" + (baseStr match {
      case "10" => ""
      case _ => s", 0, $baseStr"
    }) + ")"
  }
  override def strLength(s: expr): String =
    s"${translate(s)}.length()"
  override def strSubstring(s: expr, from: expr, to: expr): String =
    s"${translate(s)}.substr(${translate(from)}, (${translate(to)}) - (${translate(from)}))"

  override def arrayFirst(a: expr): String =
    s"${translate(a)}->front()"
  override def arrayLast(a: expr): String =
    s"${translate(a)}->back()"
}
