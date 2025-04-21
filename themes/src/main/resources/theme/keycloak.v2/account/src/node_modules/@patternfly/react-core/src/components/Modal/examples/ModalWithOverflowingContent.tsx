import React from 'react';
import { Modal, ModalVariant, Button } from '@patternfly/react-core';

export const ModalWithOverflowingContent: React.FunctionComponent = () => {
  const [isModalOpen, setIsModalOpen] = React.useState(false);

  const handleModalToggle = () => {
    setIsModalOpen(prevIsModalOpen => !prevIsModalOpen);
  };

  return (
    <React.Fragment>
      <Button variant="primary" onClick={handleModalToggle}>
        Show modal
      </Button>
      <Modal
        bodyAriaLabel="Scrollable modal content"
        tabIndex={0}
        variant={ModalVariant.small}
        title="Modal with overflowing content"
        isOpen={isModalOpen}
        onClose={handleModalToggle}
        actions={[
          <Button key="confirm" variant="primary" onClick={handleModalToggle}>
            Confirm
          </Button>,
          <Button key="cancel" variant="link" onClick={handleModalToggle}>
            Cancel
          </Button>
        ]}
      >
        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
        magna aliqua. Quis eleifend quam adipiscing vitae proin sagittis nisl rhoncus. Semper auctor neque vitae tempus.
        Diam donec adipiscing tristique risus. Augue eget arcu dictum varius duis. Ut enim blandit volutpat maecenas
        volutpat blandit aliquam. Sit amet mauris commodo quis imperdiet massa tincidunt. Habitant morbi tristique
        senectus et netus. Fames ac turpis egestas sed tempus urna. Neque laoreet suspendisse interdum consectetur
        libero id. Volutpat lacus laoreet non curabitur gravida arcu ac tortor. Porta nibh venenatis cras sed felis eget
        velit. Nullam non nisi est sit amet facilisis. Nunc mi ipsum faucibus vitae. Lorem sed risus ultricies tristique
        nulla aliquet enim tortor at. Egestas sed tempus urna et pharetra pharetra massa massa ultricies. Lacinia quis
        vel eros donec ac odio tempor orci. Malesuada fames ac turpis egestas integer eget aliquet.
        <br />
        <br />
        Neque aliquam vestibulum morbi blandit cursus risus at ultrices. Molestie at elementum eu facilisis sed odio
        morbi. Elit pellentesque habitant morbi tristique. Consequat nisl vel pretium lectus quam id leo in vitae. Quis
        varius quam quisque id diam vel quam elementum. Viverra nam libero justo laoreet sit amet cursus. Sollicitudin
        tempor id eu nisl nunc. Orci nulla pellentesque dignissim enim sit amet venenatis. Dignissim enim sit amet
        venenatis urna cursus eget. Iaculis at erat pellentesque adipiscing commodo elit. Faucibus pulvinar elementum
        integer enim neque volutpat. Nullam vehicula ipsum a arcu cursus vitae congue mauris. Nunc mattis enim ut tellus
        elementum sagittis vitae. Blandit cursus risus at ultrices. Tellus mauris a diam maecenas sed enim. Non diam
        phasellus vestibulum lorem sed risus ultricies tristique nulla.
        <br />
        <br />
        Nulla pharetra diam sit amet nisl suscipit adipiscing. Ac tortor vitae purus faucibus ornare suspendisse sed
        nisi. Sed felis eget velit aliquet sagittis id consectetur purus. Tincidunt tortor aliquam nulla facilisi cras
        fermentum. Volutpat est velit egestas dui id ornare arcu odio. Pharetra magna ac placerat vestibulum. Ultrices
        sagittis orci a scelerisque purus semper eget duis at. Nisi est sit amet facilisis magna etiam tempor orci eu.
        Convallis tellus id interdum velit. Facilisis sed odio morbi quis commodo odio aenean sed.
        <br />
        <br />
        Eu scelerisque felis imperdiet proin fermentum leo vel orci porta. Facilisi etiam dignissim diam quis enim
        lobortis scelerisque fermentum. Eleifend donec pretium vulputate sapien nec sagittis aliquam malesuada. Magna
        etiam tempor orci eu lobortis elementum. Quis auctor elit sed vulputate mi sit. Eleifend quam adipiscing vitae
        proin sagittis nisl rhoncus mattis rhoncus. Erat velit scelerisque in dictum non. Sit amet nulla facilisi morbi
        tempus iaculis urna. Enim ut tellus elementum sagittis vitae et leo duis ut. Lectus arcu bibendum at varius vel
        pharetra vel turpis. Morbi tristique senectus et netus et. Eget aliquet nibh praesent tristique magna sit amet
        purus gravida. Nisl purus in mollis nunc sed id semper risus. Id neque aliquam vestibulum morbi. Mauris a diam
        maecenas sed enim ut sem. Egestas tellus rutrum tellus pellentesque.
      </Modal>
    </React.Fragment>
  );
};
