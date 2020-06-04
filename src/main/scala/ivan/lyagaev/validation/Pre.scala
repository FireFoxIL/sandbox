package ivan.lyagaev.validation

import cats.{Applicative, Monad}

object Pre {
  trait Semigroup[A] {
    // a |+| (b |+| c) = (a |+| b) |+| c
    def combine(a: A, b: A): A
    def |+|(a: A, b: A): A = combine(a, b)
  }

  def compose[F[_]: Applicative, A, B](fa: F[A], fb: F[B]): F[(A, B)] = ???

  def sequential[F[_]: Monad, A, B](fa: F[A], f: A => F[B]): F[B] = ???
}
