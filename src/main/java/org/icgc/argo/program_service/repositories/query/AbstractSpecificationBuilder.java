/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.icgc.argo.program_service.repositories.query;

import java.util.Collection;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.springframework.data.jpa.domain.Specification;

public abstract class AbstractSpecificationBuilder<T, ID> {
  private static final String ID_FIELD = "id";
  private static final String NAME_FIELD = "name";

  protected abstract Root<T> setupFetchStrategy(Root<T> root);

  public Specification<T> buildByNameIgnoreCase(@NonNull String name) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsNameIgnoreCasePredicate(root, builder, name);
    };
  }

  public Specification<T> buildByShortName(@NonNull String shortName) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsNameShortNamePredicate(root, builder, shortName);
    };
  }

  public Specification<T> listAll(boolean distinct) {
    return (fromUser, query, builder) -> {
      query.distinct(distinct);
      val root = setupFetchStrategy(fromUser);
      return null;
    };
  }

  public Specification<T> buildById(@NonNull ID id) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsIdPredicate(root, builder, id);
    };
  }

  public Specification<T> buildByIds(@NonNull Collection<ID> ids) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return whereInIdsPredicate(root, ids);
    };
  }

  private Predicate whereInIdsPredicate(Root<T> root, Collection<ID> ids) {
    return root.get(ID_FIELD).in(ids);
  }

  private Predicate equalsIdPredicate(Root<T> root, CriteriaBuilder builder, ID id) {
    return builder.equal(root.get(ID_FIELD), id);
  }

  private Predicate equalsNameIgnoreCasePredicate(
      Root<T> root, CriteriaBuilder builder, String name) {
    return builder.equal(builder.upper(root.get(NAME_FIELD)), builder.upper(builder.literal(name)));
  }

  private Predicate equalsNameShortNamePredicate(
      Root<T> root, CriteriaBuilder builder, String shortName) {
    return builder.equal(root.get(ProgramEntity.Fields.shortName), builder.literal(shortName));
  }
}
