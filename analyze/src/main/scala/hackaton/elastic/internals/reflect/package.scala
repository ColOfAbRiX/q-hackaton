package hackaton.elastic.internals

import scala.reflect._
import scala.reflect.runtime.universe._

package object reflect {
  def fromMap[T: TypeTag: ClassTag](m: Map[String, _]): T = {
    val classMirror = runtimeMirror(classTag[T].runtimeClass.getClassLoader).reflectClass(typeOf[T].typeSymbol.asClass)
    val constructor = typeOf[T].decl(termNames.CONSTRUCTOR).asMethod

    val constructorArgs = constructor
      .paramLists.flatten.map((param: Symbol) => {
        val paramName = param.name.toString
        if (param.typeSignature <:< typeOf[Option[Any]])
          m.get(paramName)
        else
          m.getOrElse(
            paramName,
            throw new IllegalArgumentException("Map is missing required parameter named " + paramName),
          )
      })

    classMirror.reflectConstructor(constructor)(constructorArgs: _*).asInstanceOf[T]
  }
}
