package test.os

import java.nio.file.Paths

import os._
import utest._
object PathTests extends TestSuite{
  val tests = Tests {
    'Basic{
      val base = rel/'src/'main/'scala
      'Transformers - {
        if(Unix()){
          assert(
            // ammonite.Path to java.nio.file.Path
            (root/'omg).wrapped == Paths.get("/omg"),

            // java.nio.file.Path to ammonite.Path
            root/'omg == Path(Paths.get("/omg")),
            rel/'omg == RelPath(Paths.get("omg")),

            // ammonite.Path to String
            (root/'omg).toString == "/omg",
            (rel/'omg).toString == "omg",
            (up/'omg).toString == "../omg",
            (up/up/'omg).toString == "../../omg",

            // String to ammonite.Path
            root/'omg == Path("/omg"),
            rel/'omg == RelPath("omg")
          )
        }
      }

      'RelPath{
        'Constructors {
          'Symbol {
            if (Unix()){
              val rel1 = base / 'ammonite
              assert(
                rel1.segments == Seq("src", "main", "scala", "ammonite"),
                rel1.toString == "src/main/scala/ammonite"
              )
            }
          }
          'String {
            if (Unix()){
              val rel1 = base / "Path.scala"
              assert(
                rel1.segments == Seq("src", "main", "scala", "Path.scala"),
                rel1.toString == "src/main/scala/Path.scala"
              )
            }
          }
          'Combos{
            def check(rel1: RelPath) = assert(
              rel1.segments == Seq("src", "main", "scala", "sub1", "sub2"),
              rel1.toString == "src/main/scala/sub1/sub2"
            )
            'ArrayString - {
              if (Unix()){
                val arr = Array("sub1", "sub2")
                check(base / arr)
              }
            }
            'ArraySymbol - {
              if (Unix()){
                val arr = Array('sub1, 'sub2)
                check(base / arr)
              }
            }
            'SeqString - {
              if (Unix()) check(base / Seq("sub1", "sub2"))
            }
            'SeqSymbol - {
              if (Unix()) check(base / Seq('sub1, 'sub2))
            }
            'SeqSeqSeqSymbol - {
              if (Unix()){
                check(
                  base / Seq(Seq(Seq('sub1), Seq()), Seq(Seq('sub2)), Seq())
                )
              }
            }
          }
        }
        'Relativize{
          def eq[T](p: T, q: T) = assert(p == q)
          * - eq(rel/'omg/'bbq/'wtf relativeTo rel/'omg/'bbq/'wtf, rel)
          * - eq(rel/'omg/'bbq relativeTo rel/'omg/'bbq/'wtf, up)
          * - eq(rel/'omg/'bbq/'wtf relativeTo rel/'omg/'bbq, rel/'wtf)
          * - eq(rel/'omg/'bbq relativeTo rel/'omg/'bbq/'wtf, up)
          * - eq(up/'omg/'bbq relativeTo rel/'omg/'bbq, up/up/up/'omg/'bbq)
          * - intercept[PathError.NoRelativePath](rel/'omg/'bbq relativeTo up/'omg/'bbq)
        }
      }
      'AbsPath{
        val d = pwd
        val abs = d / base
        'Constructor {
          if (Unix()) assert(
            abs.toString.drop(d.toString.length) == "/src/main/scala",
            abs.toString.length > d.toString.length
          )
        }
        'Relativize{
          def eq[T](p: T, q: T) = assert(p == q)
          * - eq(root/'omg/'bbq/'wtf relativeTo root/'omg/'bbq/'wtf, rel)
          * - eq(root/'omg/'bbq relativeTo root/'omg/'bbq/'wtf, up)
          * - eq(root/'omg/'bbq/'wtf relativeTo root/'omg/'bbq, rel/'wtf)
          * - eq(root/'omg/'bbq relativeTo root/'omg/'bbq/'wtf, up)
          * - intercept[PathError.NoRelativePath](rel/'omg/'bbq relativeTo up/'omg/'bbq)
        }
      }
      'Ups{
        'RelativeUps{
          val rel2 = base/up
          assert(
            rel2 == rel/'src/'main,
            base/up/up == rel/'src,
            base/up/up/up == rel,
            base/up/up/up/up == up,
            base/up/up/up/up/up == up/up,
            up/base == up/'src/'main/'scala
          )
        }
        'AbsoluteUps{
          // Keep applying `up` and verify that the path gets
          // shorter and shorter and eventually errors.
          var abs = pwd
          var i = abs.segmentCount
          while(i > 0){
            abs /= up
            i-=1
            assert(abs.segmentCount == i)
          }
          intercept[PathError.AbsolutePathOutsideRoot.type]{ abs/up }
        }
        'RootUpBreak{
          intercept[PathError.AbsolutePathOutsideRoot.type]{ root/up }
          val x = root/"omg"
          val y = x/up
          intercept[PathError.AbsolutePathOutsideRoot.type]{ y / up }
        }
      }
      'Comparison{
        'Relative - assert(
          rel/'omg/'wtf == rel/'omg/'wtf,
          rel/'omg/'wtf != rel/'omg/'wtf/'bbq,
          rel/'omg/'wtf/'bbq startsWith rel/'omg/'wtf,
          rel/'omg/'wtf startsWith rel/'omg/'wtf,
          up/'omg/'wtf startsWith up/'omg/'wtf,
          !(rel/'omg/'wtf startsWith rel/'omg/'wtf/'bbq),
          !(up/'omg/'wtf startsWith rel/'omg/'wtf),
          !(rel/'omg/'wtf startsWith up/'omg/'wtf)
        )
        'Absolute - assert(
          root/'omg/'wtf == root/'omg/'wtf,
          root/'omg/'wtf != root/'omg/'wtf/'bbq,
          root/'omg/'wtf/'bbq startsWith root/'omg/'wtf,
          root/'omg/'wtf startsWith root/'omg/'wtf,
          !(root/'omg/'wtf startsWith root/'omg/'wtf/'bbq)
        )
        'Invalid{
          compileError("""root/'omg/'wtf < 'omg/'wtf""")
          compileError("""root/'omg/'wtf > 'omg/'wtf""")
          compileError("""'omg/'wtf < root/'omg/'wtf""")
          compileError("""'omg/'wtf > root/'omg/'wtf""")
        }
      }
    }
    'Errors{
      'InvalidChars {
        val ex = intercept[PathError.InvalidSegment](rel/'src/"Main/.scala")

        val PathError.InvalidSegment("Main/.scala", msg1) = ex

        assert(msg1.contains("[/] is not a valid character to appear in a path segment"))

        val ex2 = intercept[PathError.InvalidSegment](root/"hello"/".."/"world")

        val PathError.InvalidSegment("..", msg2) = ex2

        assert(msg2.contains("use the `up` segment from `os.up`"))
      }
      'InvalidSegments{
        intercept[PathError.InvalidSegment]{root/ "core/src/test"}
        intercept[PathError.InvalidSegment]{root/ ""}
        intercept[PathError.InvalidSegment]{root/ "."}
        intercept[PathError.InvalidSegment]{root/ ".."}
      }
      'EmptySegment {
        intercept[PathError.InvalidSegment](rel/'src / "")
        intercept[PathError.InvalidSegment](rel/'src / ".")
        intercept[PathError.InvalidSegment](rel/'src / "..")
      }
      'CannotRelativizeAbsAndRel{
        val abs = pwd
        val rel = os.rel/'omg/'wtf
        compileError("""
          abs relativeTo rel
        """).check(
          """
          abs relativeTo rel
                         ^
          """,
          "type mismatch"
        )
        compileError("""
          rel relativeTo abs
                     """).check(
            """
          rel relativeTo abs
                         ^
            """,
            "type mismatch"
          )
      }
      'InvalidCasts{
        if(Unix()){
          intercept[IllegalArgumentException](Path("omg/cow"))
          intercept[IllegalArgumentException](RelPath("/omg/cow"))
        }
      }
      'Pollution{
        // Make sure we're not polluting too much
        compileError("""'omg.ext""")
        compileError(""" "omg".ext """)
      }
    }
    'Extractors{
      'paths{
        val a/b/c/d/"omg" = pwd/'A/'B/'C/'D/"omg"
        assert(
          a == pwd/'A,
          b == "B",
          c == "C",
          d == "D"
        )

        // If the paths aren't deep enough, it
        // just doesn't match but doesn't blow up
        root/'omg match {
          case a3/b3/c3/d3/e3 => assert(false)
          case _ =>
        }
      }
    }
    'sorting{
      assert(
        Seq(root/'c, root, root/'b, root/'a).sorted == Seq(root, root/'a, root/'b, root/'c),
        Seq(up/'c, up/up/'c, rel/'b/'c, rel/'a/'c, rel/'a/'d).sorted ==
          Seq(rel/'a/'c, rel/'a/'d, rel/'b/'c, up/'c, up/up/'c)
      )
    }
    'construction{
      'success{
        if(Unix()){
          val relStr = "hello/cow/world/.."
          val absStr = "/hello/world"

          val lhs = Path(absStr)
          val rhs = root/'hello/'world
          assert(
            RelPath(relStr) == rel/'hello/'cow,
            // Path(...) also allows paths starting with ~,
            // which is expanded to become your home directory
            lhs == rhs
          )

          // You can also pass in java.io.File and java.nio.file.Path
          // objects instead of Strings when constructing paths
          val relIoFile = new java.io.File(relStr)
          val absNioFile = java.nio.file.Paths.get(absStr)

          assert(
            RelPath(relIoFile) == rel/'hello/'cow,
            Path(absNioFile) == root/'hello/'world,
            Path(relIoFile, root/'base) == root/'base/'hello/'cow
          )
        }
      }
      'basepath{
        if(Unix()){
          val relStr = "hello/cow/world/.."
          val absStr = "/hello/world"
          assert(
            FilePath(relStr) == rel/'hello/'cow,
            FilePath(absStr) == root/'hello/'world
          )
        }
      }
      'based{
        if(Unix()){
          val relStr = "hello/cow/world/.."
          val absStr = "/hello/world"
          val basePath: FilePath = FilePath(relStr)
          assert(
            Path(relStr, root/'base) == root/'base/'hello/'cow,
            Path(absStr, root/'base) == root/'hello/'world,
            Path(basePath, root/'base) == root/'base/'hello/'cow,
            Path(".", pwd).last != ""
          )
        }
      }
      'failure{
        if(Unix()){
          val relStr = "hello/.."
          intercept[java.lang.IllegalArgumentException]{
            Path(relStr)
          }

          val absStr = "/hello"
          intercept[java.lang.IllegalArgumentException]{
            RelPath(absStr)
          }

          val tooManyUpsStr = "/hello/../.."
          intercept[PathError.AbsolutePathOutsideRoot.type]{
            Path(tooManyUpsStr)
          }
        }
      }
      'symlinks{

        val names = Seq('test123, 'test124, 'test125, 'test126)
        val twd = temp.dir()

        'nestedSymlinks{
          if(Unix()) {
            names.foreach(p => os.remove.all(twd/p))
            os.makeDir.all(twd/'test123)
            os.symlink(twd/'test124, twd/'test123)
            os.symlink(twd/'test125, twd/'test124)
            os.symlink(twd/'test126, twd/'test125)
            assert(followLink(twd/'test126).get == followLink(twd/'test123).get)
            names.foreach(p => os.remove(twd/p))
            names.foreach(p => assert(!exists(twd/p)))
          }
        }

        'danglingSymlink{
          if(Unix()) {
            names.foreach(p => os.remove.all(twd/p))
            os.makeDir.all(twd/'test123)
            os.symlink(twd/'test124, twd/'test123)
            os.symlink(twd/'test125, twd/'test124)
            os.symlink(twd/'test126, twd/'test125)
            os.remove(twd / 'test123)
            assert(followLink(twd / 'test126).isEmpty)
            names.foreach(p => os.remove.all(twd / p))
            names.foreach(p => assert(!exists(twd / p)))
            names.foreach(p => os.remove.all(twd/p))
            names.foreach(p => assert(!exists(twd/p)))
          }
        }
      }
    }
  }
}
