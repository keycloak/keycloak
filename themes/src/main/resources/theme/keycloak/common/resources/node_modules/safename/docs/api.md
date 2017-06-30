safename API
============


- [safename](#safename)
- [low](#low)
- [middle](#middle)
- [dot](#dot)

<a name="safename"></a>
safename( name, space )
------------------------------------------------------------

Get safe name for files

**Parameters:**

- **name** *String*: string to transform
- **space** *String*: replace for spaces. Optional, low dash (&#x27;_&#x27;) by default
- **Return** *String*: safe name




<a name="low"></a>
low(  )
------------------------------------------------------------

Safe name with low dash '_'.

**Parameters:**



Same as `safename('your file name.txt', '_');`

<a name="middle"></a>
middle(  )
------------------------------------------------------------

Safe name with middle dash '-'.

**Parameters:**



Same as `safename('your file name.txt', '-');`

<a name="dot"></a>
dot(  )
------------------------------------------------------------

Safe name with dots '.'.

**Parameters:**



Same as `safename('your file name.txt', '.');`


