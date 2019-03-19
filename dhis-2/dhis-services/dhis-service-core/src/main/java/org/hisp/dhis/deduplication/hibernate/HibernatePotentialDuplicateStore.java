package org.hisp.dhis.deduplication.hibernate;

/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hibernate.query.Query;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.deduplication.PotentialDuplicateQuery;
import org.hisp.dhis.deduplication.PotentialDuplicateStore;

import java.util.List;

public class HibernatePotentialDuplicateStore
    extends HibernateIdentifiableObjectStore<PotentialDuplicate>
    implements PotentialDuplicateStore
{

    @Override
    public int getCountByQuery( PotentialDuplicateQuery query )
    {
        if ( query.getTei() != null )
        {
            Query<Long> hibernateQuery = getTypedQuery(
                "select count(*) from PotentialDuplicate pr where pr.teiA = '" + query.getTei() + "' or pr.teiB = '" +
                    query.getTei() + "'");

            return hibernateQuery.getSingleResult().intValue();
        }
        else
        {
            return getCount();
        }
    }

    @Override
    public List<PotentialDuplicate> getAllByQuery( PotentialDuplicateQuery query )
    {
        if ( query.getTei() != null )
        {
            Query<PotentialDuplicate> hibernateQuery = getTypedQuery(
                "from PotentialDuplicate pr where pr.teiA = '" + query.getTei() + "' or pr.teiB = '" + query.getTei() + "'" );

            return hibernateQuery.getResultList();
        }
        else
        {
            return getAll();
        }
    }

    @Override
    public boolean exists( PotentialDuplicate potentialDuplicate )
    {
        Query<Long> hibernateQuery = getTypedQuery( "select count(*) from PotentialDuplicate pd " +
            "where (pd.teiA = '" + potentialDuplicate.getTeiA() + "' and pd.teiB = '" + potentialDuplicate.getTeiB() + "') " +
            "or (pd.teiA = '" + potentialDuplicate.getTeiB() + "' and pd.teiB = '" + potentialDuplicate.getTeiA() + "') " +
            "or (pd.teiA = '" + potentialDuplicate.getTeiA() + "' and pd.teiB is null) ");

        return hibernateQuery.getSingleResult() != 0;
    }
}
