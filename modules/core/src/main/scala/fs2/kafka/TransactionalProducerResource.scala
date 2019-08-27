/*
 * Copyright 2018-2019 OVO Energy Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fs2.kafka

import cats.effect.{ConcurrentEffect, ContextShift, Resource}

import scala.concurrent.duration.FiniteDuration

/**
  * [[TransactionalProducerResource]] provides support for inferring
  * the key and value type from [[ProducerSettings]] when using
  * `transactionalProducerResource` with the following syntax.
  *
  * {{{
  * transactionalProducerResource[F].using(settings)
  * }}}
  */
final class TransactionalProducerResource[F[_]] private[kafka] (
  private val F: ConcurrentEffect[F]
) extends AnyVal {

  /**
    * Creates a new [[TransactionalKafkaProducer]] in the `Resource` context.
    * This is equivalent to using `transactionalProducerResource` directly,
    * except we're able to infer the key and value type.
    */
  def using[K, V](
    settings: ProducerSettings[F, K, V],
    transactionalId: String,
    transactionTimeout: Option[FiniteDuration] = None
  )(
    implicit context: ContextShift[F]
  ): Resource[F, TransactionalKafkaProducer[F, K, V]] =
    transactionalProducerResource(settings, transactionalId, transactionTimeout)(F, context)

  override def toString: String =
    "TransactionalProducerResource$" + System.identityHashCode(this)
}
