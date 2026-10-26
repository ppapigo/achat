import { PageHeading } from '../components/UI.jsx';
import FeedTimeline from '../components/FeedTimeline.jsx';

export default function FeedPage() {
  return <div className="feed-page"><PageHeading eyebrow="YOUR EVERYDAY CONNECTIONS" title="피드" description="나와 친구들, 최근 대화를 나눈 사람들의 소식을 만나보세요." /><FeedTimeline /></div>;
}
