import { Link } from 'react-router-dom';
import { ArrowUpRight, CalendarDays, Coins, Flag, RefreshCw, Trophy } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext.jsx';
import { useResource } from '../hooks/useResource.js';
import { number, tiers } from '../lib/format.js';
import { Empty, PageHeading, ResourceState, SectionHeading } from '../components/UI.jsx';

const seasonDate = (value) => value ? new Date(value).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }) : '—';
const tierName = (tier) => tiers[tier] || tier || '—';

export default function SeasonsPage() {
  const { user } = useAuth();
  const current = useResource('/api/seasons/current');
  const rank = useResource('/rank/me');
  const leaderboard = useResource('/rank/leaderboard?limit=20');
  const history = useResource('/api/seasons/my-history');
  const seasons = useResource('/api/seasons');
  const resources = [current, rank, leaderboard, history, seasons];
  const records = [...(history.data || [])].sort((a, b) => b.seasonId - a.seasonId);
  const seasonNames = new Map((seasons.data || []).map((season) => [season.seasonId, season]));

  return <>
    <PageHeading eyebrow="A LITTLE GROWTH, EVERY SEASON" title="시즌" description="이번 시즌의 성장과 지난 도전의 기록을 한눈에 확인해요.">
      <button className="button secondary" disabled={resources.some((resource) => resource.loading)} onClick={() => resources.forEach((resource) => resource.reload())}><RefreshCw size={16} />새로고침</button>
    </PageHeading>
    {current.error?.status === 404 ? <div className="panel"><Empty title="진행 중인 시즌이 없어요" description="새 시즌이 시작되면 이곳에서 확인할 수 있어요." /></div> : <ResourceState resource={current}>
      {current.data ? <CurrentSeason season={current.data} /> : <div className="panel"><Empty title="진행 중인 시즌이 없어요" description="새 시즌이 시작되면 이곳에서 확인할 수 있어요." /></div>}
    </ResourceState>}
    <div className="season-columns">
      <section aria-labelledby="season-rank-heading">
        <div className="section-heading"><h2 id="season-rank-heading">나의 시즌 랭크</h2><Link to="/match" className="text-link">라운드 참여하기 <ArrowUpRight size={16} /></Link></div>
        <ResourceState resource={rank}>{rank.data && <SeasonRank rank={rank.data} />}</ResourceState>
      </section>
      <section aria-labelledby="season-history-heading">
        <div className="section-heading"><h2 id="season-history-heading">지난 시즌 기록</h2>{history.data && <span className="badge">{records.length}개 시즌</span>}</div>
        <ResourceState resource={history}>
          {records.length ? <div className="season-history">{records.map((record) => {
            const season = seasonNames.get(record.seasonId);
            return <article className="panel season-record" key={record.seasonId}>
              <div className="season-record-heading"><div><h3>{season?.name || `시즌 ${record.seasonId}`}</h3><p className="muted">{season ? `${seasonDate(season.startDate)} – ${seasonDate(season.endDate)}` : `${seasonDate(record.recordedAt)} 기록`}</p></div><span className="badge soft">종료</span></div>
              <dl className="season-details"><div><dt>최종 티어</dt><dd>{tierName(record.finalTier)}</dd></div><div><dt>최종 RP</dt><dd>{number(record.finalRp)} RP</dd></div><div><dt>최고 티어</dt><dd>{tierName(record.highestTier)}</dd></div><div><dt>지급된 보상</dt><dd className="season-coins"><Coins size={16} />{number(record.rewardCoins)} 코인</dd></div></dl>
            </article>;
          })}</div> : <div className="panel"><Empty title="아직 완료한 시즌이 없어요" description="시즌이 종료되면 최종 랭크와 지급된 보상이 여기에 남아요." /></div>}
        </ResourceState>
        {seasons.error && <p className="season-note muted" role="status">시즌 이름과 기간을 불러오지 못했어요. 시즌 번호로 기록을 표시합니다. <button className="text-button" onClick={seasons.reload}>시즌 정보 다시 시도</button></p>}
      </section>
    </div>
    <section aria-labelledby="season-leaderboard-heading" className="section-space">
      <div className="section-heading"><h2 id="season-leaderboard-heading">시즌 리더보드</h2><span className="badge soft">상위 20명</span></div>
      <ResourceState resource={leaderboard}>
        {leaderboard.data?.leaderboard?.length ? <div className="panel season-leaderboard"><table><caption className="season-note muted">배치를 완료한 참여자의 현재 RP 순위예요.</caption><thead><tr><th scope="col">순위</th><th scope="col">참여자</th><th scope="col">티어</th><th scope="col">RP</th></tr></thead><tbody>{leaderboard.data.leaderboard.map((entry) => <tr key={entry.odUserId} className={entry.odUserId === user.id ? 'is-me' : ''}><td>{entry.rank}위</td><th scope="row"><span className="leaderboard-name">{entry.displayName}{entry.odUserId === user.id && <span className="badge soft">나</span>}</span><small>레벨 {entry.level}</small></th><td>{tierName(entry.tier)}</td><td>{number(entry.rp)}</td></tr>)}</tbody></table></div> : <div className="panel"><Empty title="첫 랭커를 기다리고 있어요" description="배치를 완료하면 시즌 리더보드에 참여할 수 있어요." /></div>}
      </ResourceState>
    </section>
    <SectionHeading title="시즌은 이렇게 진행돼요" />
    <div className="panel season-guide"><Flag size={22} /><p>라운드에 참여해 배치를 완료하고 RP를 쌓아보세요. 시즌이 종료되면 최종 티어와 시즌 최고 티어에 따른 코인이 자동 지급되고, 결과는 지난 시즌 기록에 저장돼요.</p></div>
  </>;
}

function CurrentSeason({ season }) {
  const progress = Math.min(100, Math.max(0, season.progressPercent || 0));
  const ended = !season.active || new Date(season.endDate).getTime() <= Date.now();
  const remaining = ended ? '종료 집계 대기' : season.remainingDays > 0 ? `${number(season.remainingDays)}일 남음` : '마지막 날';
  return <section className="panel season-banner" aria-label="현재 시즌">
    <div className="season-banner-heading"><div><span className="badge soft">{season.active ? '현재 시즌' : '종료된 시즌'}</span><h2>{season.name}</h2><p className="muted"><CalendarDays size={16} />{seasonDate(season.startDate)} – {seasonDate(season.endDate)}</p></div><span className="season-emblem" aria-hidden="true"><Trophy size={43} strokeWidth={1.5} /></span></div>
    <div className="season-progress-label"><strong>{remaining}</strong><span>{progress}% 진행</span></div><progress className="season-progress" aria-label="시즌 진행률" max={100} value={progress} />
  </section>;
}

function SeasonRank({ rank }) {
  const placementTotal = rank.placementGamesPlayed + rank.placementGamesRemaining;
  return <div className="panel season-rank">
    <div className="season-rank-title"><span className="stat-icon lavender"><Trophy size={25} /></span><div><p className="muted">현재 티어</p><h3>{tierName(rank.tier)}</h3></div><strong>{number(rank.rp)} <small>RP</small></strong></div>
    <dl className="season-details"><div><dt>시즌 최고 티어</dt><dd>{tierName(rank.seasonHighestTier)}</dd></div><div><dt>내 순위</dt><dd>{rank.placementCompleted && rank.rank > 0 ? `${number(rank.rank)}위` : '배치 진행 중'}</dd></div></dl>
    {rank.placementCompleted ? <p className="season-rank-next"><Flag size={17} />{rank.rpToNextTier > 0 ? `다음 티어까지 ${number(rank.rpToNextTier)} RP` : '최고 티어에 도달했어요!'}</p> : <div className="season-placement"><div className="season-progress-label"><strong>배치 진행</strong><span>{rank.placementGamesPlayed} / {placementTotal}회</span></div><progress className="season-progress" aria-label="배치 진행률" max={Math.max(1, placementTotal)} value={rank.placementGamesPlayed} /><p className="muted">{rank.placementGamesRemaining}회 더 참여하면 첫 티어가 정해져요.</p></div>}
  </div>;
}
