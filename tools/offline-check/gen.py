import subprocess,re,sys,os
MAP={'net.minecraft.class_332':'net.minecraft.client.gui.DrawContext','net.minecraft.class_437':'net.minecraft.client.gui.screen.Screen',
     'net.minecraft.class_1799':'net.minecraft.item.ItemStack','net.minecraft.class_2561':'net.minecraft.text.Text'}
def jp(c):
    return subprocess.run(['javap','-p','-cp','.',c],capture_output=True,text=True).stdout
def conv(t):
    for k,v in MAP.items(): t=t.replace(k,v)
    return t
def default(rt):
    rt=rt.strip()
    if rt=='void': return ''
    if rt=='boolean': return 'return false;'
    if rt in('int','long','double','float','short','byte','char'): return 'return 0;'
    if rt.startswith('java.util.List'): return 'return new java.util.ArrayList<>();'
    if rt=='java.lang.String': return 'return "";'
    if rt.endswith('[][]'): return 'return new '+rt[:-4]+'[0][];'
    if rt.endswith('[]'): return 'return new '+rt[:-2]+'[0];'
    if rt=='net.minecraft.item.ItemStack': return 'return net.minecraft.item.ItemStack.EMPTY;'
    return 'return null;'
def members(cls, simple, kind):
    out=[]
    txt=jp(cls)
    for line in txt.split('\n')[2:]:
        line=line.strip()
        if not line or line=='}' or 'lambda$' in line or 'static {}' in line: continue
        if re.search(r'class_\d+|method_\d+|field_\d+',conv(line)): continue
        line=conv(line).rstrip(';')
        line=line.replace('com.example.donutqueue.','').replace('$','.')
        if '(' in line:
            # method or ctor
            m=re.match(r'(.*?)([\w.<>]+)\((.*)\)(\s+throws .*)?$',line)
            head,name,args,thr=m.group(1),m.group(2),m.group(3),m.group(4) or ''
            if kind=='record' and (name in ('toString','hashCode','equals') or not 'static' in head and name.split('.')[-1]!=simple): continue
            ps=[a.strip() for a in args.split(',') if a.strip()]
            pl=', '.join(f'{p} a{i}' for i,p in enumerate(ps))
            name=name.split('.')[-1]
            if name==simple:  # ctor
                if kind=='record': continue
                body='super(null);' if simple.endswith('Screen') else ''
                out.append(f'  {head}{name}({pl}){thr} {{ {body} }}')
            else:
                if kind=='interface': out.append(f'  {head}{name}({pl});'); continue
                rt=re.sub(r'^((public|private|protected|static|synchronized|final|abstract|native)\s+)*','',head.strip())
                out.append(f'  {head}{name}({pl}){thr} {{ {default(rt)} }}')
        else:
            if kind=='record' and 'static' not in line: continue
            if kind=='interface':
                out.append('  '+line+' = null;' if 'String' in line else '  '+line+' = 0;'); continue
            val=''
            if 'static final' in line:
                t=line.split()[-2]
                val=' = '+('null' if t not in('int','long','double','float','boolean') else ('false' if t=='boolean' else '0'))
            out.append('  '+line+val+';')
    return out
def header(cls):
    l=jp(cls).split('\n')[1]
    return conv(l)
def gen(top, nested):
    simple=top.split('.')[-1]
    h=header(top)
    kind='interface' if ' interface ' in h else 'class'
    hdr=h.replace('com.example.donutqueue.','').replace(' {','').strip()
    hdr=re.sub(r'extends net\.minecraft\.client\.gui\.screen\.Screen','extends net.minecraft.client.gui.screen.Screen',hdr)
    hdr=re.sub(r'implements [\w.]*ModMenuApi','',hdr)
    hdr=re.sub(r'implements net\.fabricmc\.api\.ClientModInitializer','implements net.fabricmc.api.ClientModInitializer',hdr)
    body=members(top,simple,kind)
    for n in nested:
        nh=header(top+'$'+n).replace('com.example.donutqueue.','').replace('$','.').replace(' {','').strip()
        nk='record' if 'extends java.lang.Record' in nh else 'class'
        if nk=='record':
            comps=[]
            for line in jp(top+'$'+n).split('\n'):
                m=re.match(r'\s*private final (.+) (\w+);',line)
                if m: comps.append(conv(m.group(1)).replace('com.example.donutqueue.','')+' '+m.group(2))
            mods=nh.split(' final class')[0]
            body.append(f'  {mods} record {n}({", ".join(comps)}) {{}}')
        else:
            nh=re.sub(r'^((?:public |private |protected )?)(?:final )?class ',r'\1static class ',nh)
            if 'static' not in nh: nh='static '+nh
            body.append('  '+nh.replace(top.split('.')[-1]+'.','')+' {')
            body += ['  '+x for x in members(top+'$'+n,n,'class')]
            body.append('  }')
    if 'ClientModInitializer' in hdr:
        body.append('  public void onInitializeClient(){}')
    src='package com.example.donutqueue;\n'+hdr+' {\n'+'\n'.join(body)+'\n}\n'
    os.makedirs('/home/claude/stubs/src/com/example/donutqueue',exist_ok=True)
    open(f'/home/claude/stubs/src/com/example/donutqueue/{simple}.java','w').write(src)
P='com.example.donutqueue.'
for top,nested in [('DonutQueueClient',[]),('ModConfig',['BidPreset']),('QueueManager',[]),('QueueEntry',[]),('Sessions',['Session']),
                   ('PaymentLog',['Payment']),('DailyEarnings',['Day']),('Auction',[]),('PresetEditorScreen',['Entry']),('ChatStyle',[]),
                   ('AddonRegistry',[]),('DonutQueueAddon',[])]:
    gen(P+top,nested)
print('done')
