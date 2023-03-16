package com.perikov.utils

object macros:
  import cats.Show
  import cats.syntax.all.*

  opaque type TypeName[T] = String
  inline def typeName[T](using t:TypeName[T]): String = t.show
  object TypeName:
    given [T]:Show[TypeName[T]] = Show(s => s)

    inline given[T]: TypeName[T] = ${ typeNameImpl[T] }

    import scala.quoted.*

    private def typeNameImpl[T: Type](using Quotes): Expr[TypeName[T]] =
      import quotes.reflect.*
      Expr(TypeRepr.of[T].simplified.show)
