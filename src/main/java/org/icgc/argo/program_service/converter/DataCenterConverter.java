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

package org.icgc.argo.program_service.converter;

import org.icgc.argo.program_service.model.dto.DataCenterDTO;
import org.icgc.argo.program_service.model.dto.DataCenterRequestDTO;
import org.icgc.argo.program_service.model.dto.UpdateDataCenterRequestDTO;
import org.icgc.argo.program_service.model.entity.DataCenterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
    config = ConverterConfig.class,
    uses = {CommonConverter.class})
public interface DataCenterConverter {
  DataCenterConverter INSTANCE = new DataCenterConverterImpl(CommonConverter.INSTANCE);

  DataCenterDTO dataCenterToDataCenterEntity(DataCenterEntity p);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "programEntities", ignore = true)
  DataCenterEntity dataCenterToDataCenterEntity(DataCenterRequestDTO p);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "programEntities", ignore = true)
  @Mapping(target = "shortName", ignore = true)
  @Mapping(target = "submissionSongCode", ignore = true)
  DataCenterEntity dataCenterToUpdateDataCenterEntity(UpdateDataCenterRequestDTO p);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "programEntities", ignore = true)
  @Mapping(target = "shortName", ignore = true)
  @Mapping(target = "submissionSongCode", ignore = true)
  void updateDataCenter(
      DataCenterEntity updatingDataCenter, @MappingTarget DataCenterEntity dataCenterToUpdate);
}
