/* sxr -- Scala X-Ray
 * Copyright 2009 Mark Harrah
 */

package sxr

/** Represents a link to a definition.  The path is the path to the file and target is
* the symbol ID targeted.  The '#' is not included.  To get the constructed path,
* call toString. */
private class Link(val path: String, val target: Int) extends NotNull
{
	override def toString = path + "#" + target
	def retarget(newTarget: Int) = new Link(path, newTarget)
}
/** Represents a token at the lexer level with associated type information.
* 'start' is the offset of the token in the original source file.
* 'length' is the length of the token in the original source file
* 'code' is the class of the token (see Tokens in the compiler)*/
private case class Token(start: Int, length: Int, code: Int) extends NotNull with Ordered[Token]
{
	require(start >= 0)
	require(length > 0)
	/** Tokens are sorted by their start position, so that they may be searched by offset in
	* the extraction code and then processed in sequence in the annotation code. */
	def compare(other: Token) = start compare other.start
	
	private[this] var rawType: Option[TypeAttribute] = None
	private[this] var rawReference: Option[Link] = None
	private[this] var rawDefinitions: List[Int] = Nil
	/** Sets the type information for this token. */
	def tpe_=(t: TypeAttribute)
	{
		if(rawType.isEmpty)
			rawType = Some(t)
	}
	/** Sets the defining location for the symbol for this token. */
	def reference_=(l: Link)
	{
		if(rawReference.isEmpty)
			rawReference = Some(l)
	}
	/** Adds an ID for this token.  An ID is used to mark this token as the source of a symbol. */
	def +=(id: Int)
		{ rawDefinitions ::= id }
	/** Gets the type information. */
	def tpe = rawType
	/** Gets the link to the defining location for this token. */
	def reference = rawReference
	/** Gets the definition IDs. */
	def definitions = rawDefinitions
	/** True if this token has no reference to a definition and has no definitions itself. */
	def isSimple = reference.isEmpty && definitions.isEmpty
	/** True if this token has no reference to a definition, has no definitions itself, and
	* has no type information. */
	def isPlain = isSimple && tpe.isEmpty
	def collapseDefinitions(to: Int) = { rawDefinitions = to :: Nil }
	def remapReference(remap: Int => Int)
	{
		rawReference match
		{
			case Some(refLink) =>
				val newID = remap(refLink.target)
				if(newID != refLink.target)
					rawReference = Some(refLink.retarget(newID))
			case None => ()
		}
	}
}
/** Holds type information.  This class will probably change to accomodate tokeninzing types. */
private case class TypeAttribute(name: String, definition: Option[Link]) extends NotNull