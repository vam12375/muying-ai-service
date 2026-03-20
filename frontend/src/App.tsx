import { useState } from 'react';
import type { Tab } from './types';
import { Sidebar } from './components/Sidebar';
import { ChatPage } from './components/ChatPage';
import { KnowledgePage } from './components/KnowledgePage';

function App() {
  const [tab, setTab] = useState<Tab>('chat');

  return (
    <div className="flex h-screen overflow-hidden" style={{ background: '#0f0a1e' }}>
      <Sidebar activeTab={tab} onTabChange={setTab} />
      <main className="flex-1 overflow-hidden">
        {tab === 'chat' ? <ChatPage /> : <KnowledgePage />}
      </main>
    </div>
  );
}

export default App;
